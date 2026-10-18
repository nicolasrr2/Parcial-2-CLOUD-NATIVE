# Repositorio ECR para la imagen Docker del backend Spring Boot.
resource "aws_ecr_repository" "backend" {
  name                 = "dsy1107-romo3219801-backend"
  image_tag_mutability = "MUTABLE"

  image_scanning_configuration {
    scan_on_push = true
  }

  tags = {
    Project = "Pedidos360"
    Course  = "DSY1107"
  }
}

resource "aws_ecr_repository" "notificaciones" {
  name                 = "dsy1107-romo3219801-notificaciones"
  image_tag_mutability = "MUTABLE"

  image_scanning_configuration {
    scan_on_push = true
  }

  tags = {
    Project = "Pedidos360"
    Course  = "DSY1107"
  }
}

resource "aws_ecr_repository" "documentos" {
  name                 = "dsy1107-romo3219801-documentos"
  image_tag_mutability = "MUTABLE"

  image_scanning_configuration {
    scan_on_push = true
  }

  tags = {
    Project = "Pedidos360"
    Course  = "DSY1107"
  }
}

resource "aws_ecr_repository" "rabbit_admin" {
  name                 = "dsy1107-romo3219801-rabbit-admin"
  image_tag_mutability = "MUTABLE"

  image_scanning_configuration {
    scan_on_push = true
  }

  tags = {
    Project = "Pedidos360"
    Course  = "DSY1107"
  }
}
