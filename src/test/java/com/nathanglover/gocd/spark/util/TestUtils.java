package com.nathanglover.gocd.spark.util;

import com.nathanglover.gocd.spark.jsonapi.Server;
import com.nathanglover.gocd.spark.jsonapi.ServerFactory;
import com.nathanglover.gocd.spark.ruleset.Rules;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class TestUtils {

    public static ServerFactory createMockServerFactory(Server server) {
        ServerFactory factory = mock(ServerFactory.class);
        when(factory.getServer(any(Rules.class))).thenReturn(server);
        return factory;
    }

    public static String getResourceDirectory(String resource) {
        ClassLoader ldr = Thread.currentThread().getContextClassLoader();
        String url = ldr.getResource(resource).toString();
        return url.substring("file:".length(), url.lastIndexOf('/'));
    }
}
