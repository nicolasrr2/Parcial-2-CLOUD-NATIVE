output "api_url" {
  description = "Endpoint base de API Gateway"
  value       = aws_apigatewayv2_api.api_manager.api_endpoint
}

output "cognito_user_pool_id" {
  description = "ID del grupo de usuarios de Cognito"
  value       = aws_cognito_user_pool.pool.id
}

output "cognito_client_id" {
  description = "ID del cliente publico de Cognito"
  value       = aws_cognito_user_pool_client.spa.id
}

output "cognito_domain" {
  description = "Dominio completo de Cognito Hosted UI"
  value       = "https://${aws_cognito_user_pool_domain.domain.domain}.auth.us-east-1.amazoncognito.com"
}

output "cognito_issuer" {
  description = "Emisor de tokens JWT de Cognito"
  value       = "https://cognito-idp.us-east-1.amazonaws.com/${aws_cognito_user_pool.pool.id}"
}

output "cognito_group_solicitantes" {
  description = "Nombre del grupo Cognito de solicitantes"
  value       = aws_cognito_user_group.solicitantes.name
}

output "cognito_group_aprobadores" {
  description = "Nombre del grupo Cognito de aprobadores"
  value       = aws_cognito_user_group.aprobadores.name
}

output "cognito_pre_token_generation_lambda" {
  description = "Nombre de la Lambda Pre Token Generation"
  value       = aws_lambda_function.user_token_ms.function_name
}

output "ecr_backend_repository_url" {
  description = "URL del repositorio ECR del backend"
  value       = aws_ecr_repository.backend.repository_url
}

output "ecr_backend_repository_name" {
  description = "Nombre del repositorio ECR del backend"
  value       = aws_ecr_repository.backend.name
}

output "rds_endpoint" {
  value = aws_db_instance.postgres.address
}

output "rds_port" {
  value = aws_db_instance.postgres.port
}

output "rds_database_name" {
  value = aws_db_instance.postgres.db_name
}

output "rds_username" {
  value = aws_db_instance.postgres.username
}

output "backend_security_group_id" {
  value = aws_security_group.backend.id
}

output "rds_security_group_id" {
  value = aws_security_group.database.id
}

output "ecs_cluster_name" {
  value = aws_ecs_cluster.backend.name
}

output "ecs_service_name" {
  value = aws_ecs_service.backend.name
}

output "ecs_task_definition_arn" {
  value = aws_ecs_task_definition.backend.arn
}

output "alb_internal_dns" {
  value = aws_lb.backend.dns_name
}

output "alb_listener_arn" {
  value = aws_lb_listener.backend.arn
}

output "alb_target_group_arn" {
  value = aws_lb_target_group.backend.arn
}

output "apigw_vpc_link_security_group_id" {
  value = aws_security_group.apigw_vpc_link.id
}

output "backend_log_group_name" {
  value = aws_cloudwatch_log_group.backend.name
}

output "frontend_url" {
  description = "URL publica del frontend Pedidos360 desplegado en AWS Amplify"
  value       = "https://main.${aws_amplify_app.frontend.default_domain}"
}

output "frontend_amplify_app_id" {
  description = "ID de la aplicacion AWS Amplify de Pedidos360"
  value       = aws_amplify_app.frontend.id
}

output "frontend_amplify_branch_name" {
  description = "Rama productiva del frontend en AWS Amplify"
  value       = aws_amplify_branch.main.branch_name
}

output "env_frontend" {
  description = "Variables para frontend/.env.local"
  value       = <<-EOT
VITE_AWS_REGION=us-east-1
VITE_COGNITO_DOMAIN=https://${aws_cognito_user_pool_domain.domain.domain}.auth.us-east-1.amazoncognito.com
VITE_COGNITO_CLIENT_ID=${aws_cognito_user_pool_client.spa.id}
VITE_REDIRECT_URI=http://localhost:4200/
VITE_API_URL=${aws_apigatewayv2_api.api_manager.api_endpoint}
EOT
}

output "env_frontend_production" {
  description = "Variables para compilar y desplegar el frontend en produccion"
  value       = <<-EOT
VITE_AWS_REGION=us-east-1
VITE_COGNITO_DOMAIN=https://${aws_cognito_user_pool_domain.domain.domain}.auth.us-east-1.amazoncognito.com
VITE_COGNITO_CLIENT_ID=${aws_cognito_user_pool_client.spa.id}
VITE_REDIRECT_URI=https://main.${aws_amplify_app.frontend.default_domain}/
VITE_API_URL=${aws_apigatewayv2_api.api_manager.api_endpoint}
EOT
}
