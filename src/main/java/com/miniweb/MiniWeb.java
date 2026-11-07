
package com.miniweb;

import com.miniweb.controller.ArgumentResolver;
import com.miniweb.controller.ControllerRegistry;
import com.miniweb.controller.JsonConverter;
import com.miniweb.controller.ReturnValueHandler;
import com.miniweb.controller.TypeConverter;
import com.miniweb.http.HttpProtocolHandler;
import com.miniweb.routing.Dispatcher;
import com.miniweb.routing.Router;
import com.miniweb.server.MiniWebServer;

public class MiniWeb {

        public static void main(String[] args) {
                Router router = new Router();

                JsonConverter jsonConverter = new JsonConverter();

                ArgumentResolver argumentResolver = new ArgumentResolver(
                                new TypeConverter(),
                                jsonConverter);

                ReturnValueHandler returnValueHandler = new ReturnValueHandler(jsonConverter);

                ControllerRegistry registry = new ControllerRegistry(
                                router,
                                argumentResolver,
                                returnValueHandler);

                registry.registerControllers("com.example.controller");

                Dispatcher dispatcher = new Dispatcher(router);

                HttpProtocolHandler protocolHandler = new HttpProtocolHandler(dispatcher);

                MiniWebServer server = new MiniWebServer(
                                "localhost",
                                8080,
                                protocolHandler);

                server.start();
        }
}
