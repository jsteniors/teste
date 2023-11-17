package br.com.teste;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.regions.providers.AwsRegionProvider;
import software.amazon.awssdk.services.kms.KmsClient;

@Configuration
public class KmsConfiguration {

    @Bean
    KmsClient getKmsClient(AwsCredentialsProvider credentialsProvider, @Value("${spring.cloud.aws.region}") String region) {
        var kmsClient = KmsClient.builder()
                                .credentialsProvider(credentialsProvider)
                                .region(Region.of(region))
                                .build();
        return kmsClient;
    }

}
