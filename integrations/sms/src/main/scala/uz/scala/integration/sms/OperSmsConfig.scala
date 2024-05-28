package uz.scala.integration.sms

import java.net.URI

import scala.concurrent.duration.FiniteDuration

import eu.timepit.refined.types.string.NonEmptyString

case class OperSmsConfig(
    apiURL: URI,
    statusApiURL: URI,
    checkStatusTime: FiniteDuration,
    login: NonEmptyString,
    password: NonEmptyString,
    appDomain: NonEmptyString,
    enabled: Boolean = false,
  )
