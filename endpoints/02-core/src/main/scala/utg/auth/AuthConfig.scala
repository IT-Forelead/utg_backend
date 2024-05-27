package utg.auth

import utg.domain.{JwtAccessTokenKey, TokenExpiration }

case class AuthConfig(
    tokenKey: JwtAccessTokenKey,
    accessTokenExpiration: TokenExpiration,
    refreshTokenExpiration: TokenExpiration,
  )
