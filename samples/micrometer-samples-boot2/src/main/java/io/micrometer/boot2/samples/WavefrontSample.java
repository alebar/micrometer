/**
 * Copyright 2017 Pivotal Software, Inc.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
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
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.lang.Nullable;
import io.micrometer.wavefront.WavefrontConfig;
import io.micrometer.wavefront.WavefrontMeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import javax.annotation.PostConstruct;
import java.time.Instant;

@SpringBootApplication(scanBasePackageClasses = PersonController.class, scanBasePackages = {"io.micrometer.boot2.samples","io.micrometer.wavefront"})
@EnableScheduling
@RestController
public class WavefrontSample {

    private static final Logger logger = LoggerFactory.getLogger(WavefrontSample.class);
    private MeterRegistry wavefrontRegistry = null;
    // retrieve configuration parameters from application properties file
    @Value("${management.metrics.export.wavefront.enabled}")
    boolean enabled;
    @Value("${management.metrics.export.wavefront.host}")
    String host;
    @Value("${management.metrics.export.wavefront.port}")
    int port;
    @Value("${management.metrics.export.wavefront.step}")
    String step;
    @Value("${management.metrics.export.wavefront.test}")
    boolean test;

    Counter callCounter;

    public static void main(String[] args) {
        new SpringApplicationBuilder(WavefrontSample.class).profiles("wavefront").run(args);
    }

    @PostConstruct
    public void init()
    {
        wavefrontRegistry = registry();

        /**
         * adding some sample metrics
         * 1. counter type with name and point tags
         */
        callCounter = wavefrontRegistry.counter("micrometer.test.counter.http", "type", "test", "region", "US");

    }

    /**
     * simple REST mapping to denote that wavefront sample for micrometer is
     * running.
     *
     * @return
     * @throws Exception
     */
    @RequestMapping(value="/", method=RequestMethod.GET, produces={"text/html"})
    String home() throws Exception
    {
        StringBuffer buffer = new StringBuffer();
        buffer.append("<html>");
        buffer.append("<head><title>Wavefront Sample for Micrometer</title></head>");
        buffer.append("<body>");
        buffer.append("<pre>");
        buffer.append("Wavefront Sample for Micrometer");
        buffer.append("</pre>");
        buffer.append("<p>");
        buffer.append("timestamp: " + Instant.now().getEpochSecond());
        buffer.append("<br/>");
        if(callCounter != null)
        {
            callCounter.increment();
            buffer.append("call counter: " + callCounter.count());
        }
        buffer.append("</p>");
        buffer.append("</body>");
        buffer.append("</html>");
        return buffer.toString();
    }

    /**
     * internet logic to create metric registry using configuration for
     * testing on springboot environment
     * @return
     */
    public MeterRegistry registry() {
        return new WavefrontMeterRegistry(new WavefrontConfig() {

            @Override
            public boolean enabled() {
                return enabled;
            }

            @Override
            public boolean test() {
                return test;
            }

            @Override
            @Nullable
            public String get(String k)
            {
                switch(k)
                {
                    case "wavefront.proxyHost":
                        return host;
                    case "wavefront.proxyPort":
                        return Integer.toString(port);
                    case "wavefront.step":
                        return step;
                }
                return null;
            }
        }, Clock.SYSTEM);
    }
}
