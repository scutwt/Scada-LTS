package org.scada_lts.utils;

import org.apache.commons.io.IOUtils;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;
import utils.UploadFileTestUtils;

import java.io.File;
import java.io.FileInputStream;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.*;

@RunWith(Parameterized.class)
public class UploadBackgroundFileUtilsTest {

    @Parameterized.Parameters(name = "{index}: file: {0}, accept: {1}")
    public static List<Object[]> data() {
        List<Object[]> datas = new ArrayList<>();

        datas.add(new Object[] {"gifFile.gif", true});
        datas.add(new Object[] {"jpgFile.jpg", true});
        datas.add(new Object[] {"pngFile.png", true});
        datas.add(new Object[] {"svgFile.svg", true});
        datas.add(new Object[] {"354.svg", true});

        datas.add(new Object[] {"jspFile.jsp", false});
        datas.add(new Object[] {"xmlFile.xml", false});
        datas.add(new Object[] {"info.txt", false});
        datas.add(new Object[] {"abc.abc", false});
        datas.add(new Object[] {"abc", false});
        datas.add(new Object[] {"Thumbs.db", false});
        datas.add(new Object[] {"jsFile.js", false});

        datas.add(new Object[] {"jspAsSvg.svg", false});
        datas.add(new Object[] {"jspAsBmp.bmp", false});
        datas.add(new Object[] {"jspAsGif.gif", false});
        datas.add(new Object[] {"jspAsJpg.jpg", false});
        datas.add(new Object[] {"jspAsPng.png", false});

        datas.add(new Object[] {"xmlAsSvg.svg", false});
        datas.add(new Object[] {"xmlAsBmp.bmp", false});
        datas.add(new Object[] {"xmlAsGif.gif", false});
        datas.add(new Object[] {"xmlAsJpg.jpg", false});
        datas.add(new Object[] {"xmlAsPng.png", false});

        datas.add(new Object[] {"jsAsSvg.svg", false});
        datas.add(new Object[] {"jsAsBmp.bmp", false});
        datas.add(new Object[] {"jsAsGif.gif", false});
        datas.add(new Object[] {"jsAsJpg.jpg", false});
        datas.add(new Object[] {"jsAsPng.png", false});

        datas.add(new Object[] {"txt" + File.separator + "info.txt", false});
        return datas;
    }

    private final String filePath;
    private final boolean expected;

    public UploadBackgroundFileUtilsTest(String fileName, boolean expected) {
        this.filePath = UploadFileTestUtils.getResourcesPath("files", fileName);
        this.expected = expected;
    }

    @Test
    public void when_isToUploads() {

        //given:
        File file = new File(filePath);

        //when:
        boolean result = UploadFileUtils.isToUploads(file);

        //then:
        assertEquals(expected, result);
    }

    @Test
    public void when_isToUploads_MultipartFile() {

        //given:
        File file = new File(filePath);
        MultipartFile multi = toMultipartFile(file);

        //when:
        boolean result = UploadFileUtils.isToUploads(multi);

        //then:
        assertEquals(expected, result);
    }

    private static MultipartFile toMultipartFile(File file) {
        try(FileInputStream input = new FileInputStream(file)) {
            return new MockMultipartFile(file.getName(), file.getName(),
                    Files.probeContentType(file.toPath()), IOUtils.toByteArray(input));
        } catch (Exception ex) {
            return null;
        }
    }
}