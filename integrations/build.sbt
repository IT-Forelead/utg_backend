name := "integrations"
lazy val integration_aws = project.in(file("aws"))
lazy val integration_sms = project.in(file("sms"))

aggregateProjects(
  integration_aws,
  integration_sms,
)
