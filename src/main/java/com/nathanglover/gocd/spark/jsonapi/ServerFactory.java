package com.nathanglover.gocd.spark.jsonapi;

import com.nathanglover.gocd.spark.ruleset.Rules;

public class ServerFactory {

    public Server getServer(Rules rules) {
        return new Server(rules);
    }
}
