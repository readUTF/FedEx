package gg.mpl.fedex.utils;

import org.redisson.config.Config;

public class ConfigBuilder {

    String uri;

    public ConfigBuilder(String uri) {
        this.uri = uri;
    }

    public Config build() {
        Config config = new Config();
        config.useSingleServer().setAddress(uri);
        return config;
    }
}
