# Circuit breaker POC
Let's use the following definitions

- **Instance**: Single HTTP server.
- **Service**: Set of all instances for a given application.
 
We want to proof that there is a single Hystrix circuit breaker per service, as opposed to a circuit breaker per instance.
This means that one failing instance could open the circuit for the whole service, even having other healthy instances.

This is important because while migrating services from one platform to another, you may add unstable instances to a service, and you don't want to close the whole circuit just because there is an unhealthy instance on the new platform, while having plenty of healthy instances on the former platform.  

## Scenario
There are two services `ping` (1 instance) and `pong` (3 instances) that register themselves in Eureka.
The `ping` service outputs the result of making an HTTP request to the `pong` service, using round robin to use a different `pong` instance on every received request.

Let's make `pong` fail until the circuit is opened.

## Running the example
Executing the following command will start minikube, deploy Eureka, build the docker images for `ping` and `pong` and deploy them to minikube 
```bash
$ minikube start
$ eval $(minikube docker-env)
$ minikube addons enable ingress # Use ingress to expose the ping and pong services for testing and debugging
$ echo "$(minikube ip) eureka ping pong" | sudo tee -a /etc/hosts # Update /etc/hosts to add minikube IP pointing to our services 
$ kubectl create -f eureka/ # We need to start Eureka, since both ping and pong will use it
$ ./gradlew clean build buildDockerImage deploy
```

Sending requests to the `ping` service will respond successfully with responses coming from the different `pong` instances.
You can see the different `pod` name on every response
```
$ while true; do curl -i "http://ping/"; echo "\n"; sleep 0.5; done
```

Let's make one of the `pong` instances to start replying with errors
```
$ curl -i -XPOST "http://pong/"
```

Now, you should see that one of every three requests returns the fallback message, meaning that there is a `pong` instance failing.
1/3 = 33% of the requests to the `pong` service are failing.
But the circuit is not opened: we see the fallback response every time the request from `ping` to `pong` fails.

Now let's decrease to 20% the error threshold percentage for requests to the `pong` service (it was configured to 50%)
```
$ curl -i -XPOST "http://ping/env" -d hystrix.command.hello.circuitBreaker.errorThresholdPercentage=20
```

This will open the circuit since **our error rate 33% is higher than the 20% threshold**. So you'll see that all the responses go straight to the fallback, even though we have healthy `pong` instances.
```
$ curl -i "http://ping/"
```

## Conclusion
- Hystrix wraps the HTTP client. The client may or may not be a `LoadBalancer` client, which means that Hystrix is not aware of the different instances for a given service.
- When requests start to fail, if the error percentage is higher than the defined error threshold percentage for a given service, Hystrix will open the circuit.
- A potential workaround to minimize this is to add the percentage of unstable instances to the current error threshold percentage. Be careful: this will make your service tolerate more errors.
- A good healthcheck is needed for services. If a service is failing, the healthcheck should reflect that, de-registering the service from Eureka.
- When using kubernetes, use the healthcheck for the liveness probe to stop receiving traffic if the application is unhealthy.
