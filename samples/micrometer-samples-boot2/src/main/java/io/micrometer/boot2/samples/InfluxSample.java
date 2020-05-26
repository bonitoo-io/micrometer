/**
 * Copyright 2017 VMware, Inc.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * https://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.micrometer.boot2.samples;

import io.micrometer.boot2.samples.components.PersonController;
import io.micrometer.core.instrument.Clock;
import io.micrometer.core.ipc.http.OkHttpSender;
import io.micrometer.influx.InfluxConfig;
import io.micrometer.influx.InfluxMeterRegistry;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication(scanBasePackageClasses = PersonController.class)
@EnableScheduling
public class InfluxSample {
    public static void main(String[] args) {
        new SpringApplicationBuilder(InfluxSample.class).profiles("influx").run(args);
    }

    @Value("${management.metrics.export.influx.org}")
    private String org;

    @Value("${management.metrics.export.influx.bucket}")
    private String bucket;

    @Value("${management.metrics.export.influx.token}")
    private String token;

    @Bean
    public InfluxMeterRegistry influxMeterRegistry(InfluxConfig influxConfig, Clock clock) {

        OkHttpClient.Builder httpClient = new OkHttpClient.Builder();

        httpClient.addInterceptor(chain -> {
            Request original = chain.request();

            // skip others than write
            if (!original.url().pathSegments().contains("write")) {
                 return  chain.proceed(original);
            }

            HttpUrl url = original.url()
                    .newBuilder()
                    .removePathSegment(0)
                    .addEncodedPathSegments("api/v2/write")
                    .removeAllQueryParameters("db")
                    .removeAllQueryParameters("consistency")
                    .addQueryParameter("org", org)
                    .addQueryParameter("bucket", bucket)
                    .build();

            Request request = original.newBuilder()
                    .url(url)
                    .header("Authorization", "Token " + token)
                    .build();

            return chain.proceed(request);
        });

        return InfluxMeterRegistry.builder(influxConfig)
                .clock(clock)
                .httpClient(new OkHttpSender(httpClient.build()))
                .build();
    }
}
