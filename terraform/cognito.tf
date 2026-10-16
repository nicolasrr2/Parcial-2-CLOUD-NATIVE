# Grupo de usuarios para autenticar la aplicacion Angular.
resource "aws_cognito_user_pool" "pool" {
  name                = "dsy1107-romo3219801"
  username_attributes = ["email"]
  user_pool_tier      = "ESSENTIALS"

  auto_verified_attributes = ["email"]

  password_policy {
    minimum_length                   = 8
    require_lowercase                = true
    require_uppercase                = true
    require_numbers                  = true
    require_symbols                  = false
    temporary_password_validity_days = 7
  }

  admin_create_user_config {
    allow_admin_create_user_only = true
  }

  lambda_config {
    pre_token_generation_config {
      lambda_arn     = aws_lambda_function.user_token_ms.arn
      lambda_version = "V2_0"
    }
  }
}

# Resource Server que define los permisos funcionales de la API.
resource "aws_cognito_resource_server" "solicitudes" {
  identifier   = "solicitudes"
  name         = "Pedidos360 Solicitudes"
  user_pool_id = aws_cognito_user_pool.pool.id

  scope {
    scope_name        = "read"
    scope_description = "Consultar solicitudes de pedidos"
  }

  scope {
    scope_name        = "write"
    scope_description = "Crear y modificar solicitudes de pedidos"
  }

  scope {
    scope_name        = "approve"
    scope_description = "Aprobar solicitudes de pedidos"
  }
}

# Grupos funcionales de Cognito; la autorizacion efectiva la aplica el backend.
resource "aws_cognito_user_group" "solicitantes" {
  name         = "solicitantes"
  user_pool_id = aws_cognito_user_pool.pool.id
  description  = "Usuarios que crean y consultan solicitudes"
}

resource "aws_cognito_user_group" "aprobadores" {
  name         = "aprobadores"
  user_pool_id = aws_cognito_user_pool.pool.id
  description  = "Usuarios que consultan y aprueban solicitudes"
}

# Dominio clasico de Hosted UI de Cognito.
resource "aws_cognito_user_pool_domain" "domain" {
  domain                = "dsy1107-romo3219801-${lower(replace(aws_cognito_user_pool.pool.id, "_", "-"))}"
  user_pool_id          = aws_cognito_user_pool.pool.id
  managed_login_version = 1
}

# Cliente publico para el navegador; no almacena un secreto.
resource "aws_cognito_user_pool_client" "spa" {
  name         = "spa-angular"
  user_pool_id = aws_cognito_user_pool.pool.id

  generate_secret                      = false
  allowed_oauth_flows_user_pool_client = true
  allowed_oauth_flows                  = ["code"]
  supported_identity_providers         = ["COGNITO"]
  allowed_oauth_scopes                 = ["openid", "email", "profile"]

  callback_urls = [
    "http://localhost:4200/",
    "https://main.${aws_amplify_app.frontend.default_domain}/"
  ]
  logout_urls = [
    "http://localhost:4200/",
    "https://main.${aws_amplify_app.frontend.default_domain}/"
  ]

  access_token_validity  = 60
  id_token_validity      = 60
  refresh_token_validity = 1

  token_validity_units {
    access_token  = "minutes"
    id_token      = "minutes"
    refresh_token = "days"
  }
}

# Empaquetado determinista de la Lambda sin dependencias externas.
data "archive_file" "user_token_ms" {
  type        = "zip"
  source_file = "${path.module}/lambda/user-token-ms/index.mjs"
  output_path = "${path.module}/lambda/user-token-ms.zip"
}

data "aws_iam_role" "lab_role" {
  name = "LabRole"
}

resource "aws_lambda_function" "user_token_ms" {
  function_name    = "dsy1107-romo3219801-user-token-ms"
  role             = data.aws_iam_role.lab_role.arn
  runtime          = "nodejs22.x"
  handler          = "index.handler"
  filename         = data.archive_file.user_token_ms.output_path
  source_code_hash = data.archive_file.user_token_ms.output_base64sha256
  timeout          = 5
}

resource "aws_lambda_permission" "cognito_user_token_ms" {
  statement_id  = "AllowCognitoInvokeUserTokenMs"
  action        = "lambda:InvokeFunction"
  function_name = aws_lambda_function.user_token_ms.function_name
  principal     = "cognito-idp.amazonaws.com"
  source_arn    = aws_cognito_user_pool.pool.arn
}
