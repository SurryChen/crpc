package com.somecode.core.compressAlgorithm.gzip;

import com.somecode.common.spi.core.CompressAlgorithm;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

public class GzipCompressAlgorithm implements CompressAlgorithm {

    @Override
    public byte[] compress(byte[] input) {
        try {
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream(input.length);
            GZIPOutputStream gZIPOutputStream = new GZIPOutputStream(byteArrayOutputStream);
            gZIPOutputStream.write(input);
            gZIPOutputStream.close();
            byteArrayOutputStream.close();
            return byteArrayOutputStream.toByteArray();
        } catch (IOException unused) {
            return input;
        }
    }

    @Override
    public byte[] uncompress(byte[] input) {
        try {
            ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(input);
            GZIPInputStream gZIPInputStream = new GZIPInputStream(byteArrayInputStream);
            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(gZIPInputStream, StandardCharsets.UTF_8));
            StringBuilder sb2 = new StringBuilder();
            while (true) {
                String readLine = bufferedReader.readLine();
                if (readLine != null) {
                    sb2.append(readLine);
                } else {
                    bufferedReader.close();
                    gZIPInputStream.close();
                    byteArrayInputStream.close();
                    return sb2.toString().getBytes();
                }
            }
        } catch (IOException unused) {
            unused.printStackTrace();
            return input;
        }
    }
}
