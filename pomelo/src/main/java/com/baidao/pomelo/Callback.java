package com.baidao.pomelo;

/**
 * @author rjhy
 * @created on 16-8-22
 * @desc desc
 */
public interface Callback<T> {
    public void onSuccess(T t);

    public void onError();
}
