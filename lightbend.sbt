// credentials can be retrieved from https://www.lightbend.com/product/lightbend-reactive-platform/credentials
inThisBuild(Seq(
  credentials += Credentials(Path.userHome / ".lightbend" / "commercial.credentials"),
  resolvers += "com-mvn" at "https://repo.lightbend.com/commercial-releases/",
  resolvers += Resolver.url("com-ivy",
    url("https://repo.lightbend.com/commercial-releases/"))(Resolver.ivyStylePatterns)
))
