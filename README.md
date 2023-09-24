### SPRINGBOOT PROJECT

- Implementing [RESTful Web Services](https://spring.io/guides/tutorials/rest/)
- Implementing [SpringData JPA](https://spring.io/projects/spring-data-jpa)
- Implementing [Service Registry](https://spring.io/guides/gs/service-registration-and-discovery/)
- Implementing [Exception Handling](https://spring.io/blog/2013/11/01/exception-handling-in-spring-mvc)
- Implementing [Feign Client](https://cloud.spring.io/spring-cloud-netflix/multi/multi_spring-cloud-feign.html) & [RestTemplate](https://docs.spring.io/spring-android/docs/current/reference/html/rest-template.html)
- Implementing [API Gateway](https://spring.io/projects/spring-cloud-gateway)
- Implementing Distributed Log Tracing using [Zipkin](https://zipkin.io/)
- Implementing [Circuit Breaker](https://resilience4j.readme.io/docs/circuitbreaker) in API
- Implementing Rate Limiter in API Gateway using [Resilience 4j]((https://resilience4j.readme.io/docs/ratelimiter)) and Redis
- Implementing Spring Security With [Okta](https://developer.okta.com/blog/2020/08/14/spring-gateway-patterns)

<hr>

## Application Architecture
![_Microservices with Spring Boot, Spring Cloud, Docker, K8s](https://github.com/isml26/springboot-example/assets/62605922/7da7ac55-6352-42e8-a3e7-ebca5db5776d)


## Example Usage of commands in docker environment

1. Before building docker image make sure we have jar file of application, run in application folder contains Dockerfile :
    ```shell
    mvn clean install
2. To build docker image use:
    ```shell
   docker build -t ismail26/configserver:0.0.1 . 
   
![docker](https://github.com/isml26/springboot-example/assets/62605922/a3174f07-23cd-40b4-a4ac-0c2b7bae1c7c)

3. We should define environment variables instead of using localhost:
    ```shell
    docker run -d -p 9296:9296 -e EUREKA_SERVER_ADDRESS=http://host.docker.internal:8761/eureka --name configserver 102e2067e7fd

* ![images](https://github.com/isml26/springboot-example/assets/62605922/4ebe7434-fd83-4147-a655-d68d195c80ce)
* ![containers](https://github.com/isml26/springboot-example/assets/62605922/b8ec6192-8202-467d-9ca8-3f8c3ccd8dda)
* ![logs](https://github.com/isml26/springboot-example/assets/62605922/3c74d65b-6415-48cc-992a-0d0f9e34aee8)

4. We can define multiple tags while building the image
   ```shell
    docker build -t ismail26/configserver:0.0.1 -t ismail26/configserver:latest .