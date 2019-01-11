# Generic Service Broker

Use this generic service broker to expose services to your
Cloud native applications, using the
[Open Service Broker API](https://www.openservicebrokerapi.org/).
Using a service broker, your applications are not tied to a specific
service: you are free to update the target service, without having to
replace the service definition in client applications.

Service brokers are featured in modern Cloud platforms such as
Cloud Foundry or Kubernetes (Service Catalog). Thanks to this project,
you are free to expose any services to your Cloud native applications.

Let's say you have a database running in a legacy server. You want to
access this database from your Cloud Native applications: you may use
a direct reference to this database. Doing that, all your applications
need to know where to find the database (host name or IP address), and
how to connect to the service (credentials).

Using a service broker allows you to remove that hard dependency between
your database and your applications. The service broker is the only
component which knows how to connect to the database. Your applications
now just ask the service broker to provide a link to some database service.

If you update the target service definition (for example:
the database is running on a different machine, the credentials are changed),
your Cloud native applications do not require an update.

Use this project to connect any services (especially those running outside of
your Cloud platform) to your applications.

## How to use it?

This project requires JDK 8:
```bash
$ ./mvnw clean package
```

### Deploy to Cloud Foundry

Just push the main JAR file to Cloud Foundry:
```bash
$ cf push
```

Make sure you use a different application name if you need to
expose different services. Update the manifest if required.

You need to set environment variables to define the service you
want to expose:
```bash
$ cf set-env generic-service-broker KEY VALUE
```

The following properties can be set:

| Key                         | Value                                              |
|-----------------------------|----------------------------------------------------|
| BROKER_SERVICE_URL          | URL to the exposed service (eg: jdbc:mysql://mydb) |
| BROKER_SERVICE_ID           | Service identifier (no space)                      |
| BROKER_SERVICE_NAME         | Service name                                       |
| BROKER_SERVICE_DESCRIPTION  | Service description                                |
| BROKER_SERVICE_ICON         | URL to the service icon                            |
| BROKER_SERVICE_TAG          | Service tag                                        |
| BROKER_SERVICE_PROVIDER     | String identifying the service provider            |
| BROKER_CREDENTIALS_PASSWORD | Admin password                                     |

Don't forget to restage the broker when you're done setting properties:
```bash
$ cf restage generic-service-broker
```

Register this application as a space-scoped service broker:
```bash
$ cf create-service-broker generic-service-broker \
  admin $ADMIN_PASSWORD https://path.to.service.broker --space-scoped
```

You are now ready to use your service broker.
Just create a new service instance, and bind it to your applications.

## Contribute

Contributions are always welcome!

Feel free to open issues & send PR.

## License

Copyright &copy; 2019 [Pivotal Software, Inc](https://pivotal.io).

This project is licensed under the [Apache Software License version 2.0](https://www.apache.org/licenses/LICENSE-2.0).
