package org.example;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.*;


@ExtendWith(MockitoExtension.class)
class ServerTest {
    @Test
    void choosePort_test() throws IOException {
        File fileSettings = new File("C:/Users/Юрий/IdeaProjects/diplom_2/server/src/main/resources/settings_test.txt");
        //
        String actual = Server.choosePort(fileSettings);
        String expected = "Current port is: 26";
        //
        Assertions.assertEquals(actual, expected);
    }
}