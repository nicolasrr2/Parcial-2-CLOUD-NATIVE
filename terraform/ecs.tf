# Logs de la tarea ECS del backend.
resource "aws_cloudwatch_log_group" "backend" {
  name              = "/ecs/dsy1107-romo3219801-backend"
  retention_in_days = 1

  tags = {
    Project = "Pedidos360"
    Course  = "DSY1107"
  }
}

# La contrasena de RDS se entrega al contenedor mediante Secrets Manager.
resource "aws_secretsmanager_secret" "rds_password" {
  name                    = "dsy1107-romo3219801-rds-password"
  recovery_window_in_days = 0
}

resource "aws_secretsmanager_secret_version" "rds_password" {
  secret_id     = aws_secretsmanager_secret.rds_password.id
  secret_string = random_password.rds_master.result
}

# Security Group reservado para el futuro VPC Link de API Gateway.
resource "aws_security_group" "apigw_vpc_link" {
  name        = "dsy1107-romo3219801-apigw-vpc-link-sg"
  description = "Salida del VPC Link hacia el ALB interno"
  vpc_id      = data.aws_vpc.default.id

  egress {
    description = "Salida IPv4 general"
    from_port   = 0
    to_port     = 0
    protocol    = "-1"
    cidr_blocks = ["0.0.0.0/0"]
  }

  tags = {
    Project = "Pedidos360"
    Course  = "DSY1107"
  }
}

# El ALB solo acepta trafico desde el VPC Link.
resource "aws_security_group" "alb" {
  name        = "dsy1107-romo3219801-alb-sg"
  description = "Acceso interno al ALB de Pedidos360"
  vpc_id      = data.aws_vpc.default.id

  egress {
    description = "Salida IPv4 general"
    from_port   = 0
    to_port     = 0
    protocol    = "-1"
    cidr_blocks = ["0.0.0.0/0"]
  }

  tags = {
    Project = "Pedidos360"
    Course  = "DSY1107"
  }
}

resource "aws_vpc_security_group_ingress_rule" "alb_from_apigw" {
  security_group_id            = aws_security_group.alb.id
  referenced_security_group_id = aws_security_group.apigw_vpc_link.id
  from_port                    = 80
  to_port                      = 80
  ip_protocol                  = "tcp"
  description                  = "HTTP desde API Gateway VPC Link"
}

resource "aws_vpc_security_group_ingress_rule" "backend_from_alb" {
  security_group_id            = aws_security_group.backend.id
  referenced_security_group_id = aws_security_group.alb.id
  from_port                    = 8080
  to_port                      = 8080
  ip_protocol                  = "tcp"
  description                  = "HTTP desde el ALB interno"
}

resource "aws_lb" "backend" {
  name               = "dsy1107-romo3219801-alb"
  internal           = true
  load_balancer_type = "application"
  security_groups    = [aws_security_group.alb.id]
  subnets            = data.aws_subnets.default.ids

  enable_deletion_protection = false

  tags = {
    Project = "Pedidos360"
    Course  = "DSY1107"
  }
}

resource "aws_lb_target_group" "backend" {
  name                 = "dsy1107-romo3219801-tg"
  port                 = 8080
  protocol             = "HTTP"
  target_type          = "ip"
  vpc_id               = data.aws_vpc.default.id
  deregistration_delay = 10

  health_check {
    enabled             = true
    path                = "/actuator/health"
    protocol            = "HTTP"
    matcher             = "200"
    interval            = 30
    timeout             = 5
    healthy_threshold   = 2
    unhealthy_threshold = 2
  }
}

resource "aws_lb_listener" "backend" {
  load_balancer_arn = aws_lb.backend.arn
  port              = 80
  protocol          = "HTTP"

  default_action {
    type             = "forward"
    target_group_arn = aws_lb_target_group.backend.arn
  }
}

resource "aws_ecs_cluster" "backend" {
  name = "dsy1107-romo3219801-cluster"

  tags = {
    Project = "Pedidos360"
    Course  = "DSY1107"
  }
}

resource "aws_ecs_task_definition" "backend" {
  family                   = "dsy1107-romo3219801-backend"
  network_mode             = "awsvpc"
  requires_compatibilities = ["FARGATE"]
  cpu                      = "256"
  memory                   = "512"
  execution_role_arn       = data.aws_iam_role.lab_role.arn
  task_role_arn            = data.aws_iam_role.lab_role.arn

  container_definitions = jsonencode([
    {
      name      = "pedidos360-backend"
      image     = "${aws_ecr_repository.backend.repository_url}:latest"
      essential = true

      portMappings = [
        {
          containerPort = 8080
          hostPort      = 8080
          protocol      = "tcp"
        }
      ]

      environment = [
        {
          name  = "DB_URL"
          value = "jdbc:postgresql://${aws_db_instance.postgres.address}:5432/${aws_db_instance.postgres.db_name}"
        },
        {
          name  = "DB_USERNAME"
          value = aws_db_instance.postgres.username
        },
        {
          name  = "COGNITO_ISSUER"
          value = "https://cognito-idp.us-east-1.amazonaws.com/${aws_cognito_user_pool.pool.id}"
        },
        {
          name  = "COGNITO_CLIENT_ID"
          value = aws_cognito_user_pool_client.spa.id
        },
        {
          name  = "RABBITMQ_ADDRESSES"
          value = "${aws_instance.rabbitmq.private_ip}:5672,${aws_instance.rabbitmq.private_ip}:5673,${aws_instance.rabbitmq.private_ip}:5674"
        }
      ]

      secrets = [
        {
          name      = "DB_PASSWORD"
          valueFrom = aws_secretsmanager_secret.rds_password.arn
        },
        {
          name      = "RABBITMQ_USERNAME"
          valueFrom = "${aws_secretsmanager_secret.rabbitmq_credentials.arn}:username::"
        },
        {
          name      = "RABBITMQ_PASSWORD"
          valueFrom = "${aws_secretsmanager_secret.rabbitmq_credentials.arn}:password::"
        }
      ]

      logConfiguration = {
        logDriver = "awslogs"
        options = {
          "awslogs-group"         = aws_cloudwatch_log_group.backend.name
          "awslogs-region"        = "us-east-1"
          "awslogs-stream-prefix" = "ecs"
        }
      }
    }
  ])

  depends_on = [aws_secretsmanager_secret_version.rds_password, aws_secretsmanager_secret_version.rabbitmq_credentials]
}

resource "aws_ecs_service" "backend" {
  name             = "dsy1107-romo3219801-backend-service"
  cluster          = aws_ecs_cluster.backend.id
  task_definition  = aws_ecs_task_definition.backend.arn
  desired_count    = 1
  launch_type      = "FARGATE"
  platform_version = "LATEST"

  health_check_grace_period_seconds = 120

  network_configuration {
    subnets          = data.aws_subnets.default.ids
    security_groups  = [aws_security_group.backend.id]
    assign_public_ip = true
  }

  load_balancer {
    target_group_arn = aws_lb_target_group.backend.arn
    container_name   = "pedidos360-backend"
    container_port   = 8080
  }

  depends_on = [aws_lb_listener.backend]

  deployment_minimum_healthy_percent = 50
  deployment_maximum_percent         = 200

  tags = {
    Project = "Pedidos360"
    Course  = "DSY1107"
  }
}

resource "aws_cloudwatch_log_group" "notificaciones" {
  name              = "/ecs/dsy1107-romo3219801-notificaciones"
  retention_in_days = 1

  tags = {
    Project = "Pedidos360"
    Course  = "DSY1107"
  }
}

resource "aws_cloudwatch_log_group" "documentos" {
  name              = "/ecs/dsy1107-romo3219801-documentos"
  retention_in_days = 1

  tags = {
    Project = "Pedidos360"
    Course  = "DSY1107"
  }
}

resource "aws_cloudwatch_log_group" "rabbit_admin" {
  name              = "/ecs/dsy1107-romo3219801-rabbit-admin"
  retention_in_days = 1

  tags = {
    Project = "Pedidos360"
    Course  = "DSY1107"
  }
}

resource "random_password" "rabbitmq_app_password" {
  length           = 24
  special          = true
  override_special = "!#$%&*()-_=+[]{}<>:?"
}

resource "random_password" "rabbitmq_erlang_cookie" {
  length  = 32
  special = false
}

resource "aws_secretsmanager_secret" "rabbitmq_credentials" {
  name                    = "dsy1107-romo3219801-rabbitmq-credentials"
  recovery_window_in_days = 0

  tags = {
    Project = "Pedidos360"
    Course  = "DSY1107"
  }
}

resource "aws_secretsmanager_secret_version" "rabbitmq_credentials" {
  secret_id = aws_secretsmanager_secret.rabbitmq_credentials.id
  secret_string = jsonencode({
    username      = "admin"
    password      = random_password.rabbitmq_app_password.result
    erlang_cookie = random_password.rabbitmq_erlang_cookie.result
  })
}

resource "aws_security_group" "microservices" {
  name        = "dsy1107-romo3219801-microservices-sg"
  description = "Microservicios Pedidos360 sin puertos publicos"
  vpc_id      = data.aws_vpc.default.id

  egress {
    description = "Salida IPv4 general"
    from_port   = 0
    to_port     = 0
    protocol    = "-1"
    cidr_blocks = ["0.0.0.0/0"]
  }

  tags = {
    Project = "Pedidos360"
    Course  = "DSY1107"
  }
}

resource "aws_security_group" "rabbitmq" {
  name        = "dsy1107-romo3219801-rabbitmq-sg"
  description = "RabbitMQ EC2 host interno con AMQP restringido a la VPC"
  vpc_id      = data.aws_vpc.default.id

  ingress {
    description     = "AMQP desde backend ECS"
    from_port       = 5672
    to_port         = 5672
    protocol        = "tcp"
    security_groups = [aws_security_group.backend.id, aws_security_group.microservices.id]
  }

  ingress {
    description     = "AMQP desde backend ECS"
    from_port       = 5673
    to_port         = 5673
    protocol        = "tcp"
    security_groups = [aws_security_group.backend.id, aws_security_group.microservices.id]
  }

  ingress {
    description     = "AMQP desde backend ECS"
    from_port       = 5674
    to_port         = 5674
    protocol        = "tcp"
    security_groups = [aws_security_group.backend.id, aws_security_group.microservices.id]
  }

  egress {
    description = "Salida IPv4 general"
    from_port   = 0
    to_port     = 0
    protocol    = "-1"
    cidr_blocks = ["0.0.0.0/0"]
  }

  tags = {
    Project = "Pedidos360"
    Course  = "DSY1107"
  }
}

data "aws_ami" "amazon_linux_2023" {
  most_recent = true
  owners      = ["amazon"]

  filter {
    name   = "name"
    values = ["al2023-ami-2023.*-x86_64"]
  }

  filter {
    name   = "architecture"
    values = ["x86_64"]
  }
}

data "aws_subnets" "rabbitmq_us_east_1a" {
  filter {
    name   = "vpc-id"
    values = [data.aws_vpc.default.id]
  }

  filter {
    name   = "availability-zone"
    values = ["us-east-1a"]
  }

  filter {
    name   = "default-for-az"
    values = ["true"]
  }
}

resource "aws_instance" "rabbitmq" {
  ami                         = data.aws_ami.amazon_linux_2023.id
  instance_type               = "t3.medium"
  subnet_id                   = data.aws_subnets.rabbitmq_us_east_1a.ids[0]
  associate_public_ip_address = true
  vpc_security_group_ids      = [aws_security_group.rabbitmq.id]
  iam_instance_profile        = "LabInstanceProfile"

  user_data = <<-EOT
    #!/bin/bash
    set -euo pipefail

    dnf install -y docker
    aws --version
    systemctl enable --now docker
    usermod -a -G docker ec2-user

    mkdir -p /tmp/rabbitmq
    SECRET_JSON=$(aws secretsmanager get-secret-value --secret-id ${aws_secretsmanager_secret.rabbitmq_credentials.name} --query SecretString --output text)
    export RABBITMQ_USERNAME=$(printf '%s' "$SECRET_JSON" | python3 -c 'import sys, json; print(json.load(sys.stdin)["username"])')
    export RABBITMQ_PASSWORD=$(printf '%s' "$SECRET_JSON" | python3 -c 'import sys, json; print(json.load(sys.stdin)["password"])')
    export RABBITMQ_ERLANG_COOKIE=$(printf '%s' "$SECRET_JSON" | python3 -c 'import sys, json; print(json.load(sys.stdin)["erlang_cookie"])')

    cat > /tmp/rabbitmq/rabbitmq.conf <<EOF
cluster_formation.peer_discovery_backend = classic_config
cluster_formation.classic_config.nodes.1 = rabbit@rabbitmq-1
cluster_formation.classic_config.nodes.2 = rabbit@rabbitmq-2
cluster_formation.classic_config.nodes.3 = rabbit@rabbitmq-3
cluster_partition_handling = autoheal
default_user = $${RABBITMQ_USERNAME}
default_pass = $${RABBITMQ_PASSWORD}
loopback_users.guest = false
EOF

    docker network inspect rabbitmq_cluster >/dev/null 2>&1 || docker network create --driver bridge rabbitmq_cluster

    docker volume inspect rabbitmq_data_1 >/dev/null 2>&1 || docker volume create rabbitmq_data_1
    docker volume inspect rabbitmq_data_2 >/dev/null 2>&1 || docker volume create rabbitmq_data_2
    docker volume inspect rabbitmq_data_3 >/dev/null 2>&1 || docker volume create rabbitmq_data_3

    docker rm -f pedidos360-rabbitmq-1 pedidos360-rabbitmq-2 pedidos360-rabbitmq-3 >/dev/null 2>&1 || true

    docker run -d --name pedidos360-rabbitmq-1 --hostname rabbitmq-1 --restart unless-stopped \
      --network rabbitmq_cluster \
      -p 5672:5672 -p 15672:15672 \
      -e RABBITMQ_DEFAULT_USER="$RABBITMQ_USERNAME" \
      -e RABBITMQ_DEFAULT_PASS="$RABBITMQ_PASSWORD" \
      -e RABBITMQ_ERLANG_COOKIE="$RABBITMQ_ERLANG_COOKIE" \
      -v rabbitmq_data_1:/var/lib/rabbitmq \
      -v /tmp/rabbitmq/rabbitmq.conf:/etc/rabbitmq/rabbitmq.conf:ro \
      rabbitmq:4-management

    docker run -d --name pedidos360-rabbitmq-2 --hostname rabbitmq-2 --restart unless-stopped \
      --network rabbitmq_cluster \
      -p 5673:5672 -p 15673:15672 \
      -e RABBITMQ_DEFAULT_USER="$RABBITMQ_USERNAME" \
      -e RABBITMQ_DEFAULT_PASS="$RABBITMQ_PASSWORD" \
      -e RABBITMQ_ERLANG_COOKIE="$RABBITMQ_ERLANG_COOKIE" \
      -v rabbitmq_data_2:/var/lib/rabbitmq \
      -v /tmp/rabbitmq/rabbitmq.conf:/etc/rabbitmq/rabbitmq.conf:ro \
      rabbitmq:4-management

    docker run -d --name pedidos360-rabbitmq-3 --hostname rabbitmq-3 --restart unless-stopped \
      --network rabbitmq_cluster \
      -p 5674:5672 -p 15674:15672 \
      -e RABBITMQ_DEFAULT_USER="$RABBITMQ_USERNAME" \
      -e RABBITMQ_DEFAULT_PASS="$RABBITMQ_PASSWORD" \
      -e RABBITMQ_ERLANG_COOKIE="$RABBITMQ_ERLANG_COOKIE" \
      -v rabbitmq_data_3:/var/lib/rabbitmq \
      -v /tmp/rabbitmq/rabbitmq.conf:/etc/rabbitmq/rabbitmq.conf:ro \
      rabbitmq:4-management

    for container in pedidos360-rabbitmq-1 pedidos360-rabbitmq-2 pedidos360-rabbitmq-3; do
      ready=false
      for attempt in $(seq 1 60); do
        if docker exec "$container" rabbitmq-diagnostics -q ping >/dev/null 2>&1; then
          ready=true
          break
        fi
        sleep 5
      done
      if [ "$ready" != true ]; then
        echo "RabbitMQ container did not become ready: $container" >&2
        exit 1
      fi
    done

    docker exec pedidos360-rabbitmq-1 rabbitmqctl cluster_status
  EOT

  user_data_replace_on_change = true
  depends_on                  = [aws_secretsmanager_secret_version.rabbitmq_credentials]

  tags = {
    Name    = "dsy1107-romo3219801-rabbitmq"
    Project = "Pedidos360"
    Course  = "DSY1107"
  }
}

resource "aws_ecs_task_definition" "notificaciones" {
  family                   = "dsy1107-romo3219801-notificaciones"
  network_mode             = "awsvpc"
  requires_compatibilities = ["FARGATE"]
  cpu                      = "256"
  memory                   = "512"
  execution_role_arn       = data.aws_iam_role.lab_role.arn
  task_role_arn            = data.aws_iam_role.lab_role.arn

  container_definitions = jsonencode([
    {
      name      = "pedidos360-notificaciones"
      image     = "${aws_ecr_repository.notificaciones.repository_url}:latest"
      essential = true

      portMappings = [
        {
          containerPort = 8082
          hostPort      = 8082
          protocol      = "tcp"
        }
      ]

      environment = [
        {
          name  = "RABBITMQ_ADDRESSES"
          value = "${aws_instance.rabbitmq.private_ip}:5672,${aws_instance.rabbitmq.private_ip}:5673,${aws_instance.rabbitmq.private_ip}:5674"
        }
      ]

      secrets = [
        {
          name      = "RABBITMQ_USERNAME"
          valueFrom = "${aws_secretsmanager_secret.rabbitmq_credentials.arn}:username::"
        },
        {
          name      = "RABBITMQ_PASSWORD"
          valueFrom = "${aws_secretsmanager_secret.rabbitmq_credentials.arn}:password::"
        }
      ]

      logConfiguration = {
        logDriver = "awslogs"
        options = {
          "awslogs-group"         = aws_cloudwatch_log_group.notificaciones.name
          "awslogs-region"        = "us-east-1"
          "awslogs-stream-prefix" = "ecs"
        }
      }
    }
  ])

  depends_on = [aws_secretsmanager_secret_version.rabbitmq_credentials]
}

resource "aws_ecs_task_definition" "documentos" {
  family                   = "dsy1107-romo3219801-documentos"
  network_mode             = "awsvpc"
  requires_compatibilities = ["FARGATE"]
  cpu                      = "256"
  memory                   = "512"
  execution_role_arn       = data.aws_iam_role.lab_role.arn
  task_role_arn            = data.aws_iam_role.lab_role.arn

  container_definitions = jsonencode([
    {
      name      = "pedidos360-documentos"
      image     = "${aws_ecr_repository.documentos.repository_url}:latest"
      essential = true

      portMappings = [
        {
          containerPort = 8083
          hostPort      = 8083
          protocol      = "tcp"
        }
      ]

      environment = [
        {
          name  = "RABBITMQ_ADDRESSES"
          value = "${aws_instance.rabbitmq.private_ip}:5672,${aws_instance.rabbitmq.private_ip}:5673,${aws_instance.rabbitmq.private_ip}:5674"
        }
      ]

      secrets = [
        {
          name      = "RABBITMQ_USERNAME"
          valueFrom = "${aws_secretsmanager_secret.rabbitmq_credentials.arn}:username::"
        },
        {
          name      = "RABBITMQ_PASSWORD"
          valueFrom = "${aws_secretsmanager_secret.rabbitmq_credentials.arn}:password::"
        }
      ]

      logConfiguration = {
        logDriver = "awslogs"
        options = {
          "awslogs-group"         = aws_cloudwatch_log_group.documentos.name
          "awslogs-region"        = "us-east-1"
          "awslogs-stream-prefix" = "ecs"
        }
      }
    }
  ])

  depends_on = [aws_secretsmanager_secret_version.rabbitmq_credentials]
}

resource "aws_ecs_task_definition" "rabbit_admin" {
  family                   = "dsy1107-romo3219801-rabbit-admin"
  network_mode             = "awsvpc"
  requires_compatibilities = ["FARGATE"]
  cpu                      = "256"
  memory                   = "512"
  execution_role_arn       = data.aws_iam_role.lab_role.arn
  task_role_arn            = data.aws_iam_role.lab_role.arn

  container_definitions = jsonencode([
    {
      name      = "pedidos360-rabbit-admin"
      image     = "${aws_ecr_repository.rabbit_admin.repository_url}:latest"
      essential = true

      portMappings = [
        {
          containerPort = 8084
          hostPort      = 8084
          protocol      = "tcp"
        }
      ]

      environment = [
        {
          name  = "RABBITMQ_ADDRESSES"
          value = "${aws_instance.rabbitmq.private_ip}:5672,${aws_instance.rabbitmq.private_ip}:5673,${aws_instance.rabbitmq.private_ip}:5674"
        }
      ]

      secrets = [
        {
          name      = "RABBITMQ_USERNAME"
          valueFrom = "${aws_secretsmanager_secret.rabbitmq_credentials.arn}:username::"
        },
        {
          name      = "RABBITMQ_PASSWORD"
          valueFrom = "${aws_secretsmanager_secret.rabbitmq_credentials.arn}:password::"
        }
      ]

      logConfiguration = {
        logDriver = "awslogs"
        options = {
          "awslogs-group"         = aws_cloudwatch_log_group.rabbit_admin.name
          "awslogs-region"        = "us-east-1"
          "awslogs-stream-prefix" = "ecs"
        }
      }
    }
  ])

  depends_on = [aws_secretsmanager_secret_version.rabbitmq_credentials]
}

resource "aws_ecs_service" "notificaciones" {
  name             = "dsy1107-romo3219801-notificaciones-service"
  cluster          = aws_ecs_cluster.backend.id
  task_definition  = aws_ecs_task_definition.notificaciones.arn
  desired_count    = 1
  launch_type      = "FARGATE"
  platform_version = "LATEST"

  network_configuration {
    subnets          = data.aws_subnets.default.ids
    security_groups  = [aws_security_group.microservices.id]
    assign_public_ip = true
  }

  deployment_minimum_healthy_percent = 50
  deployment_maximum_percent         = 200

  tags = {
    Project = "Pedidos360"
    Course  = "DSY1107"
  }
}

resource "aws_ecs_service" "documentos" {
  name             = "dsy1107-romo3219801-documentos-service"
  cluster          = aws_ecs_cluster.backend.id
  task_definition  = aws_ecs_task_definition.documentos.arn
  desired_count    = 1
  launch_type      = "FARGATE"
  platform_version = "LATEST"

  network_configuration {
    subnets          = data.aws_subnets.default.ids
    security_groups  = [aws_security_group.microservices.id]
    assign_public_ip = true
  }

  deployment_minimum_healthy_percent = 50
  deployment_maximum_percent         = 200

  tags = {
    Project = "Pedidos360"
    Course  = "DSY1107"
  }
}

resource "aws_ecs_service" "rabbit_admin" {
  name             = "dsy1107-romo3219801-rabbit-admin-service"
  cluster          = aws_ecs_cluster.backend.id
  task_definition  = aws_ecs_task_definition.rabbit_admin.arn
  desired_count    = 1
  launch_type      = "FARGATE"
  platform_version = "LATEST"

  network_configuration {
    subnets          = data.aws_subnets.default.ids
    security_groups  = [aws_security_group.microservices.id]
    assign_public_ip = true
  }

  deployment_minimum_healthy_percent = 50
  deployment_maximum_percent         = 200

  tags = {
    Project = "Pedidos360"
    Course  = "DSY1107"
  }
}
