# API HTTP privada para el backend de Pedidos360.
resource "aws_apigatewayv2_api" "api_manager" {
  name          = "dsy1107-romo3219801-api"
  protocol_type = "HTTP"

  cors_configuration {
    allow_origins = [
      "http://localhost:4200",
      "https://main.${aws_amplify_app.frontend.default_domain}"
    ]
    allow_methods = ["GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"]
    allow_headers = ["authorization", "content-type"]
    max_age       = 300
  }
}

# Subnets compatibles con API Gateway VPC Link V2 en us-east-1.
data "aws_subnets" "apigw_vpc_link" {
  filter {
    name   = "vpc-id"
    values = [data.aws_vpc.default.id]
  }

  filter {
    name = "availability-zone-id"
    values = [
      "use1-az1",
      "use1-az2",
      "use1-az4",
      "use1-az5",
      "use1-az6"
    ]
  }
}

# Enlace privado entre API Gateway y el ALB interno.
resource "aws_apigatewayv2_vpc_link" "backend" {
  name               = "dsy1107-romo3219801-vpc-link"
  subnet_ids         = data.aws_subnets.apigw_vpc_link.ids
  security_group_ids = [aws_security_group.apigw_vpc_link.id]

  tags = {
    Project = "Pedidos360"
    Course  = "DSY1107"
  }
}

# Integracion privada con el listener HTTP del ALB interno.
resource "aws_apigatewayv2_integration" "backend" {
  api_id                 = aws_apigatewayv2_api.api_manager.id
  integration_type       = "HTTP_PROXY"
  integration_method     = "ANY"
  integration_uri        = aws_lb_listener.backend.arn
  connection_type        = "VPC_LINK"
  connection_id          = aws_apigatewayv2_vpc_link.backend.id
  payload_format_version = "1.0"

  request_parameters = {
    "overwrite:path" = "$request.path"
  }
}

# Autoriza el endpoint protegido con tokens JWT emitidos por Cognito.
resource "aws_apigatewayv2_authorizer" "cognito" {
  api_id           = aws_apigatewayv2_api.api_manager.id
  authorizer_type  = "JWT"
  identity_sources = ["$request.header.Authorization"]
  name             = "cognito-jwt"

  jwt_configuration {
    issuer   = "https://cognito-idp.us-east-1.amazonaws.com/${aws_cognito_user_pool.pool.id}"
    audience = [aws_cognito_user_pool_client.spa.id]
  }
}

# Consultar las solicitudes propias.
resource "aws_apigatewayv2_route" "solicitudes_mias" {
  api_id               = aws_apigatewayv2_api.api_manager.id
  route_key            = "GET /solicitudes/mias"
  target               = "integrations/${aws_apigatewayv2_integration.backend.id}"
  authorization_type   = "JWT"
  authorizer_id        = aws_apigatewayv2_authorizer.cognito.id
  authorization_scopes = ["solicitudes/read"]
}

# Consultar solicitudes pendientes.
resource "aws_apigatewayv2_route" "solicitudes_pendientes" {
  api_id               = aws_apigatewayv2_api.api_manager.id
  route_key            = "GET /solicitudes/pendientes"
  target               = "integrations/${aws_apigatewayv2_integration.backend.id}"
  authorization_type   = "JWT"
  authorizer_id        = aws_apigatewayv2_authorizer.cognito.id
  authorization_scopes = ["solicitudes/read"]
}

# Consultar una solicitud especifica.
resource "aws_apigatewayv2_route" "solicitud_por_id" {
  api_id               = aws_apigatewayv2_api.api_manager.id
  route_key            = "GET /solicitudes/{id}"
  target               = "integrations/${aws_apigatewayv2_integration.backend.id}"
  authorization_type   = "JWT"
  authorizer_id        = aws_apigatewayv2_authorizer.cognito.id
  authorization_scopes = ["solicitudes/read"]
}

# Crear una solicitud.
resource "aws_apigatewayv2_route" "crear_solicitud" {
  api_id               = aws_apigatewayv2_api.api_manager.id
  route_key            = "POST /solicitudes"
  target               = "integrations/${aws_apigatewayv2_integration.backend.id}"
  authorization_type   = "JWT"
  authorizer_id        = aws_apigatewayv2_authorizer.cognito.id
  authorization_scopes = ["solicitudes/write"]
}

# Actualizar una solicitud.
resource "aws_apigatewayv2_route" "actualizar_solicitud" {
  api_id               = aws_apigatewayv2_api.api_manager.id
  route_key            = "PUT /solicitudes/{id}"
  target               = "integrations/${aws_apigatewayv2_integration.backend.id}"
  authorization_type   = "JWT"
  authorizer_id        = aws_apigatewayv2_authorizer.cognito.id
  authorization_scopes = ["solicitudes/write"]
}

# Eliminar una solicitud.
resource "aws_apigatewayv2_route" "eliminar_solicitud" {
  api_id               = aws_apigatewayv2_api.api_manager.id
  route_key            = "DELETE /solicitudes/{id}"
  target               = "integrations/${aws_apigatewayv2_integration.backend.id}"
  authorization_type   = "JWT"
  authorizer_id        = aws_apigatewayv2_authorizer.cognito.id
  authorization_scopes = ["solicitudes/write"]
}

# Resolver la decision de una solicitud.
resource "aws_apigatewayv2_route" "decidir_solicitud" {
  api_id               = aws_apigatewayv2_api.api_manager.id
  route_key            = "PATCH /solicitudes/{id}/decision"
  target               = "integrations/${aws_apigatewayv2_integration.backend.id}"
  authorization_type   = "JWT"
  authorizer_id        = aws_apigatewayv2_authorizer.cognito.id
  authorization_scopes = ["solicitudes/approve"]
}

# El stage por defecto publica automaticamente los cambios de rutas.
resource "aws_apigatewayv2_stage" "default" {
  api_id      = aws_apigatewayv2_api.api_manager.id
  name        = "$default"
  auto_deploy = true
}
