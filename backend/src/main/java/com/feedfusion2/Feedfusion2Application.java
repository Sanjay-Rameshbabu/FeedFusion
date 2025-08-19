package com.feedfusion2;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
// Import the annotation
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;

@SpringBootApplication
// Add this annotation and specify the package containing your repositories
@EnableMongoRepositories(basePackages = "com.feedfusion2.repository")
public class Feedfusion2Application {

    public static void main(String[] args) {
        SpringApplication.run(Feedfusion2Application.class, args);
    }

}
