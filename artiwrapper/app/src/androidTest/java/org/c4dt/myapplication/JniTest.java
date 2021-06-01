package org.c4dt.myapplication;

import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.c4dt.artiwrapper.JniApi;
import org.junit.Test;
import org.junit.runner.RunWith;

import static org.junit.Assert.assertEquals;

@RunWith(AndroidJUnit4.class)
public class JniTest {

    @Test
    public void helloRust() {
        JniApi jniApi = new JniApi();
        assertEquals("Hello world!", jniApi.hello("world"));
    }
}
