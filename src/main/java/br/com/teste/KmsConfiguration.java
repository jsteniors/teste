package br.com.teste;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.regions.providers.AwsRegionProvider;
import software.amazon.awssdk.services.kms.KmsClient;

@Configuration
public class KmsConfiguration {

    @Bean
    KmsClient getKmsClient(AwsCredentialsProvider credentialsProvider, AwsRegionProvider regionProvider) {
        var kmsClient = KmsClient.builder()
                                .credentialsProvider(credentialsProvider)
                                .region(regionProvider.getRegion())
                                .build();
        return kmsClient;
    }

}
