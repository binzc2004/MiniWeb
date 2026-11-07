
package com.miniweb.controller;

import com.miniweb.annotation.PathVariable;
import com.miniweb.annotation.RequestBody;
import com.miniweb.annotation.RequestParam;
import com.miniweb.http.HttpRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.List;
import java.util.Map;

/**
 * Controller 方法参数解析器。
 *
 * @author zicheng bin
 */
public class ArgumentResolver {

    private static final Logger log =
            LoggerFactory.getLogger(ArgumentResolver.class);

    private final TypeConverter typeConverter;
    private final JsonConverter jsonConverter;

    public ArgumentResolver(
            TypeConverter typeConverter,
            JsonConverter jsonConverter
    ) {
        this.typeConverter = typeConverter;
        this.jsonConverter = jsonConverter;
    }

    /**
     * 根据方法参数上的注解解析请求参数。
     */
    public Object[] resolveArguments(
            Method method,
            HttpRequest request,
            Map<String, String> pathVariables
    ) {
        Parameter[] parameters = method.getParameters();
        Object[] arguments = new Object[parameters.length];

        for (int i = 0; i < parameters.length; i++) {
            Parameter parameter = parameters[i];

            PathVariable pathVariable =
                    parameter.getAnnotation(PathVariable.class);

            RequestParam requestParam =
                    parameter.getAnnotation(RequestParam.class);

            RequestBody requestBody =
                    parameter.getAnnotation(RequestBody.class);

            int annotationCount =
                    countAnnotations(parameter.getAnnotations());

            if (annotationCount != 1) {
                throw new IllegalArgumentException(
                        "Each parameter must have exactly one binding annotation: "
                                + parameter.getName()
                );
            }

            if (pathVariable != null) {
                String value = pathVariables.get(pathVariable.value());

                if (value == null) {
                    throw new IllegalArgumentException(
                            "Missing path variable: " + pathVariable.value()
                    );
                }

                arguments[i] = typeConverter.convert(
                        value,
                        parameter.getType()
                );
            } else if (requestParam != null) {
                List<String> values =
                        request.getQueryParameter(requestParam.value());

                if (values.isEmpty()) {
                    throw new IllegalArgumentException(
                            "Missing query parameter: " + requestParam.value()
                    );
                }

                if (values.size() != 1) {
                    throw new IllegalArgumentException(
                            "Multiple values for query parameter: "
                                    + requestParam.value()
                    );
                }

                arguments[i] = typeConverter.convert(
                        values.get(0),
                        parameter.getType()
                );
            } else if (requestBody != null) {
                String body = request.getBodyAsString();

                if (body.isBlank()) {
                    throw new IllegalArgumentException(
                            "Request body cannot be empty"
                    );
                }

                if (parameter.getType() == String.class) {
                    arguments[i] = body;
                } else {
                    arguments[i] = jsonConverter.fromJson(
                            body,
                            parameter.getType()
                    );
                }
            }

            log.debug(
                    "Resolved parameter {} for method {}",
                    parameter.getName(),
                    method.getName()
            );
        }

        return arguments;
    }

    /**
     * 统计参数上的绑定注解数量。
     */
    private int countAnnotations(Annotation[] annotations) {
        int count = 0;

        for (Annotation annotation : annotations) {
            if (annotation instanceof PathVariable
                    || annotation instanceof RequestParam
                    || annotation instanceof RequestBody) {
                count++;
            }
        }

        return count;
    }
}
