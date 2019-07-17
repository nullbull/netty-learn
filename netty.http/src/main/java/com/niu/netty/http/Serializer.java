package com.niu.netty.http;

public interface Serializer {

    byte[] serialize(Object object);

    <T> T deserialize(Class<T> clazz, byte[] bytes);
}
