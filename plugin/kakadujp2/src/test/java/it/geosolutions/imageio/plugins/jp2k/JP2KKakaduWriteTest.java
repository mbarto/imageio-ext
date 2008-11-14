package it.geosolutions.imageio.plugins.jp2k;

import it.geosolutions.resources.TestData;

import java.awt.Rectangle;
import java.awt.Transparency;
import java.awt.color.ColorSpace;
import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.awt.image.ComponentColorModel;
import java.awt.image.DataBuffer;
import java.awt.image.DataBufferInt;
import java.awt.image.DataBufferUShort;
import java.awt.image.Raster;
import java.awt.image.RenderedImage;
import java.awt.image.SampleModel;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.LinkedList;
import java.util.logging.Logger;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;
import javax.media.jai.JAI;
import javax.media.jai.ParameterBlockJAI;
import javax.media.jai.RenderedOp;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import kdu_jni.KduException;

import com.sun.imageio.plugins.bmp.BMPImageReaderSpi;

public class JP2KKakaduWriteTest extends TestCase {

    /** The LOGGER for this class. */
    private static final Logger LOGGER = Logger
            .getLogger("it.geosolutions.imageio.plugins.jp2k");

    public JP2KKakaduWriteTest(String name) {
        super(name);
    }

    private final static double lossLessQuality = 1;

    private final static double lossyQuality = 0.01;

    private final static String testPath;

    private final static String fileSeparator = System
            .getProperty("file.separator");

    class TestConfiguration {
        String outputFileName;

        boolean writeCodeStreamOnly;

        double quality;

        boolean useJAI;

        JP2KKakaduImageWriteParam param = null;

        public TestConfiguration(String fileName,
                final boolean writeCodestreamOnly, final double quality,
                final boolean useJAI, final JP2KKakaduImageWriteParam param) {
            outputFileName = fileName;
            this.writeCodeStreamOnly = writeCodestreamOnly;
            this.quality = quality;
            this.useJAI = useJAI;
            this.param = param;
        }
    }

    static {
        String path = System.getProperty("data.path");
        if (path != null && path.length() > 1) {
            path = path.replace("\\", "/");
            final char lastChar = path.charAt(path.length() - 1);
            if (lastChar == '/')
                testPath = path;
            else
                testPath = path + "/";
        } else
            testPath = System.getProperty("java.io.tmpdir");
    }

    private final static String[] files = new String[] { "IM-0001-30023.bmp",
            "IM-0001-0008.bmp", "IM-0001-0010.bmp", "IM-0001-0014.bmp",
            "OT-MONO2-8-hip.bmp", "MR-MONO2-8-16x-heart (12).bmp",
            "8-bit Uncompressed Gray.bmp" };

    private final static String inputFileName = testPath;

    // private final static String inputFileName12bit = testPath + "test1.jp2";

    private final static String outputFileName = testPath + fileSeparator
            + "out" + fileSeparator;

    public void testKakaduWriter() throws KduException, FileNotFoundException,
            IOException {

        for (String fileName : files) {
            final String filePath = inputFileName + fileName;
            final File file = new File(filePath);
            if (!file.exists()) {
                LOGGER
                        .warning("Unable to find the file "
                                + filePath
                                + "\n Be sure you have properly specified the \"data.path\" property linking to the location where test data is available. \n This test will be skipped");
                continue;
            }

            final String suffix = fileName.substring(0, fileName.length() - 4);
            LinkedList<TestConfiguration> configs = new LinkedList<TestConfiguration>();

            configs.add(new TestConfiguration(outputFileName + "_" + suffix,
                    true, lossLessQuality, false, null));

            configs.add(new TestConfiguration(outputFileName + "_" + suffix,
                    false, lossLessQuality, false, null));
            configs.add(new TestConfiguration(outputFileName + "_" + suffix,
                    true, lossyQuality, false, null));
            configs.add(new TestConfiguration(outputFileName + "_" + suffix,
                    false, lossyQuality, false, null));
            configs.add(new TestConfiguration(
                    outputFileName + "_JAI_" + suffix, true, lossLessQuality,
                    true, null));
            configs.add(new TestConfiguration(
                    outputFileName + "_JAI_" + suffix, false, lossLessQuality,
                    true, null));
//            configs.add(new TestConfiguration(
//                    outputFileName + "_JAI_" + suffix, true, lossyQuality,
//                    true, null));
//            configs.add(new TestConfiguration(
//                    outputFileName + "_JAI_" + suffix, false, lossyQuality,
//                    true, null));

            JP2KKakaduImageWriteParam param = new JP2KKakaduImageWriteParam();
            final int levels = 2;
            param.setCLevels(levels);

            configs.add(new TestConfiguration(outputFileName + "_" + levels + "levels_"
                    + suffix, true, lossLessQuality, false, param));
            configs.add(new TestConfiguration(outputFileName + "_" + levels + "levels_"
                    + suffix, false, lossLessQuality, false, param));
            configs.add(new TestConfiguration(outputFileName + "_" + levels + "levels_"
                    + suffix, true, lossyQuality, false, param));
            configs.add(new TestConfiguration(outputFileName + "_" + levels + "levels_"
                    + suffix, false, lossyQuality, false, param));

            for (TestConfiguration config : configs) {

                final ParameterBlockJAI pbjImageRead = new ParameterBlockJAI(
                        "ImageRead");
                ImageReader reader = ImageIO.getImageReaders(
                        ImageIO.createImageInputStream(file)).next();

                pbjImageRead.setParameter("reader", reader);
                pbjImageRead.setParameter("Input", file);
                RenderedOp image = JAI.create("ImageRead", pbjImageRead);

                write(config.outputFileName, image, config.writeCodeStreamOnly,
                        config.quality, config.useJAI, config.param);
            }
        }
    }

    public void testKakaduWriterParam() throws KduException,
            FileNotFoundException, IOException {

        final String fileName = files[0];
        final String filePath = inputFileName + fileName;
        final File file = new File(filePath);
        if (!file.exists()) {
            LOGGER.warning("Unable to find the file " + filePath
                    + "\n This test will be skipped");
            return;
        }
        final String suffix = fileName.substring(0, fileName.length() - 4);

        LinkedList<TestConfiguration> configs = new LinkedList<TestConfiguration>();

        JP2KKakaduImageWriteParam param = new JP2KKakaduImageWriteParam();
        param.setSourceRegion(new Rectangle(100, 0, 450, 800));
        param.setSourceSubsampling(2, 3, 0, 0);

        configs.add(new TestConfiguration(outputFileName + "_pp_" + suffix,
                true, lossLessQuality, false, param));
        configs.add(new TestConfiguration(outputFileName + "_pp_" + suffix,
                false, lossLessQuality, false, param));
        configs.add(new TestConfiguration(outputFileName + "_pp_" + suffix,
                true, lossyQuality, false, param));
        configs.add(new TestConfiguration(outputFileName + "_pp_" + suffix,
                false, lossyQuality, false, param));
        configs.add(new TestConfiguration(outputFileName + "_pp_JAI_" + suffix,
                true, lossLessQuality, true, param));
        configs.add(new TestConfiguration(outputFileName + "_pp_JAI_" + suffix,
                false, lossLessQuality, true, param));
//        configs.add(new TestConfiguration(outputFileName + "_pp_JAI_" + suffix,
//                true, lossyQuality, true, param));
//        configs.add(new TestConfiguration(outputFileName + "_pp_JAI_" + suffix,
//                false, lossyQuality, true, param));

        for (TestConfiguration config : configs) {

            final ParameterBlockJAI pbjImageRead = new ParameterBlockJAI(
                    "ImageRead");
            ImageReader reader = ImageIO.getImageReaders(
                    ImageIO.createImageInputStream(file)).next();

            pbjImageRead.setParameter("reader", reader);
            pbjImageRead.setParameter("Input", file);
            RenderedOp image = JAI.create("ImageRead", pbjImageRead);
            write(config.outputFileName, image, config.writeCodeStreamOnly,
                    config.quality, config.useJAI, config.param);
        }
    }

    private static synchronized void write(String file, RenderedImage bi,
            boolean codeStreamOnly, double quality, boolean useJAI,
            JP2KKakaduImageWriteParam addParam) throws IOException {
        file += "_Q" + quality + (codeStreamOnly ? ".j2c" : ".jp2");
        final ImageOutputStream outputStream = ImageIO
                .createImageOutputStream(new File(file));
        JP2KKakaduImageWriteParam param = new JP2KKakaduImageWriteParam();
        param.setQuality(quality);
        param.setWriteCodeStreamOnly(codeStreamOnly);

        if (addParam != null) {
            param.setSourceRegion(addParam.getSourceRegion());
            param.setSourceSubsampling(addParam.getSourceXSubsampling(),
                    addParam.getSourceYSubsampling(), addParam
                            .getSubsamplingXOffset(), addParam
                            .getSubsamplingYOffset());
            param.setCLevels(addParam.getCLevels());
            param.setQualityLayers(addParam.getQualityLayers());
        }

        if (!useJAI) {
            final ImageWriter writer = new JP2KKakaduImageWriterSpi()
                    .createWriterInstance();

            // final ImageWriter writer = new
            // J2KImageWriterSpi().createWriterInstance();
            writer.setOutput(outputStream);
            // J2KImageWriteParam ioparam = (J2KImageWriteParam)
            // writer.getDefaultWriteParam();
            // ioparam.setWriteCodeStreamOnly(true);
            // ioparam.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
            // ioparam.setCompressionType("JPEG2000");
            // ioparam.setCompressionQuality((float)quality);
            // ioparam.setEncodingRate((quality)*24);
            // writer.write(null, new IIOImage(bi, null, null), ioparam);
            writer.write(null, new IIOImage(bi, null, null), param);
            writer.dispose();
        } else {
            final ParameterBlockJAI pbjImageWrite = new ParameterBlockJAI(
                    "ImageWrite");

            final ImageWriter writer = new JP2KKakaduImageWriterSpi()
                    .createWriterInstance();
            pbjImageWrite.setParameter("writer", writer);
            pbjImageWrite.setParameter("output", outputStream);
            pbjImageWrite.setParameter("writeParam", param);
            pbjImageWrite.addSource(bi);
            RenderedOp image = JAI.create("ImageWrite", pbjImageWrite);
        }
    }

    public static void main(java.lang.String[] args) {
        junit.textui.TestRunner.run(suite());
    }

    public static Test suite() {
        TestSuite suite = new TestSuite();

        suite.addTest(new JP2KKakaduWriteTest("testKakaduWriter"));

        suite.addTest(new JP2KKakaduWriteTest("testKakaduWriterParam"));

        suite.addTest(new JP2KKakaduWriteTest("testRGB"));

        suite.addTest(new JP2KKakaduWriteTest("test12BitGray"));

        suite.addTest(new JP2KKakaduWriteTest("test16BitGray"));

        suite.addTest(new JP2KKakaduWriteTest("test24BitGray"));

        suite.addTest(new JP2KKakaduWriteTest("testPalettedRGB"));
        
        suite.addTest(new JP2KKakaduWriteTest("testReducedMemory"));

        return suite;
    }


    public static void testReducedMemory() throws IOException {
        System.setProperty(JP2KKakaduImageWriter.MAX_BUFFER_SIZE_KEY, "16K");
        final ColorSpace cs = ColorSpace.getInstance(ColorSpace.CS_GRAY);
        ColorModel cm = new ComponentColorModel(cs, new int[] { 24 }, false,
                false, Transparency.OPAQUE, DataBuffer.TYPE_INT);
        final int w = 2048;
        final int h = 2048;
        SampleModel sm = cm.createCompatibleSampleModel(w, h);
        final int bufferSize = w * h;
        final int[] bufferValues = new int[bufferSize];
        for (int i = 0; i < h; i++) {
            for (int j = 0; j < w; j++)
//                bufferValues[j + (i * h)] = (int) (j + i) * (16777216 / 4096);
                bufferValues[j + (i * h)] = (int) (Math.random() * 16777215d);
        }
        DataBuffer imageBuffer = new DataBufferInt(bufferValues, bufferSize);
        BufferedImage bi = new BufferedImage(cm, Raster.createWritableRaster(
                sm, imageBuffer, null), false, null);

        write(outputFileName + "_RM", bi, true, lossLessQuality);
        write(outputFileName + "_RM", bi, false, lossLessQuality);
        write(outputFileName + "_RM", bi, true, lossyQuality);
        write(outputFileName + "_RM", bi, false, lossyQuality);
    }
    
    public static void test12BitGray() throws IOException {
        final ColorSpace cs = ColorSpace.getInstance(ColorSpace.CS_GRAY);
        ColorModel cm = new ComponentColorModel(cs, new int[] { 12 }, false,
                false, Transparency.OPAQUE, DataBuffer.TYPE_USHORT);
        final int w = 512;
        final int h = 512;
        SampleModel sm = cm.createCompatibleSampleModel(w, h);
        final int bufferSize = w * h;
        final short[] bufferValues = new short[bufferSize];
        for (int i = 0; i < h; i++) {
            for (int j = 0; j < w; j++)
//                bufferValues[j + (i * h)] = (short) ((j + i) * (4096 / 1024));
            bufferValues[j + (i * h)] = (short) (Math.random() * 4095d);
        }
        DataBuffer imageBuffer = new DataBufferUShort(bufferValues, bufferSize);
        BufferedImage bi = new BufferedImage(cm, Raster.createWritableRaster(
                sm, imageBuffer, null), false, null);

        write(outputFileName + "_gray12", bi, true, lossLessQuality);
        write(outputFileName + "_gray12", bi, false, lossLessQuality);
        write(outputFileName + "_gray12", bi, true, lossyQuality);
        write(outputFileName + "_gray12", bi, false, lossyQuality);
        write(outputFileName + "_JAI_gray12", bi, true, lossLessQuality, true);
        write(outputFileName + "_JAI_gray12", bi, false, lossLessQuality, true);
//        write(outputFileName + "_JAI_gray12", bi, true, lossyQuality, true);
//        write(outputFileName + "_JAI_gray12", bi, false, lossyQuality, true);
    }

    public static void test16BitGray() throws IOException {
        final ColorSpace cs = ColorSpace.getInstance(ColorSpace.CS_GRAY);
        ColorModel cm = new ComponentColorModel(cs, new int[] { 16 }, false,
                false, Transparency.OPAQUE, DataBuffer.TYPE_USHORT);
        final int w = 512;
        final int h = 512;
        SampleModel sm = cm.createCompatibleSampleModel(w, h);
        final int bufferSize = w * h;
        final short[] bufferValues = new short[bufferSize];
        for (int i = 0; i < h; i++) {
            for (int j = 0; j < w; j++)
//                bufferValues[j + (i * h)] = (short) ((j + i) * (65536 / 1024));
                bufferValues[j + (i * h)] = (short) (Math.random() * 65535);
        }

        DataBuffer imageBuffer = new DataBufferUShort(bufferValues, bufferSize);
        BufferedImage bi = new BufferedImage(cm, Raster.createWritableRaster(
                sm, imageBuffer, null), false, null);

        write(outputFileName + "_gray16", bi, true, lossLessQuality);
        write(outputFileName + "_gray16", bi, false, lossLessQuality);
        write(outputFileName + "_gray16", bi, true, lossyQuality);
        write(outputFileName + "_gray16", bi, false, lossyQuality);
        write(outputFileName + "_JAI_gray16", bi, true, lossLessQuality, true);
        write(outputFileName + "_JAI_gray16", bi, false, lossLessQuality, true);
//        write(outputFileName + "_JAI_gray16", bi, true, lossyQuality, true);
//        write(outputFileName + "_JAI_gray16", bi, false, lossyQuality, true);
    }

    public static void test24BitGray() throws IOException {
        final ColorSpace cs = ColorSpace.getInstance(ColorSpace.CS_GRAY);
        ColorModel cm = new ComponentColorModel(cs, new int[] { 24 }, false,
                false, Transparency.OPAQUE, DataBuffer.TYPE_INT);
        final int w = 512;
        final int h = 512;
        SampleModel sm = cm.createCompatibleSampleModel(w, h);
        final int bufferSize = w * h;
        final int[] bufferValues = new int[bufferSize];
        for (int i = 0; i < h; i++) {
            for (int j = 0; j < w; j++)
//                bufferValues[j + (i * h)] = (int) (j + i) * (16777216 / 1024);
            bufferValues[j + (i * h)] = (int) (Math.random() * 16777215d);
        }
        DataBuffer imageBuffer = new DataBufferInt(bufferValues, bufferSize);
        BufferedImage bi = new BufferedImage(cm, Raster.createWritableRaster(
                sm, imageBuffer, null), false, null);

        write(outputFileName + "_gray24", bi, true, lossLessQuality);
        write(outputFileName + "_gray24", bi, false, lossLessQuality);
        write(outputFileName + "_gray24", bi, true, lossyQuality);
        write(outputFileName + "_gray24", bi, false, lossyQuality);
        write(outputFileName + "_JAI_gray24", bi, true, lossLessQuality, true);
        write(outputFileName + "_JAI_gray24", bi, false, lossLessQuality, true);
//        write(outputFileName + "_JAI_gray24", bi, true, lossyQuality, true);
//        write(outputFileName + "_JAI_gray24", bi, false, lossyQuality, true);
    }

    public void testRGB() throws IOException {
        final File file = TestData.file(this, "RGB24.bmp");
        final ImageReader reader = new BMPImageReaderSpi()
                .createReaderInstance();
        reader.setInput(ImageIO.createImageInputStream(file));
        BufferedImage bi = reader.read(0);
        write(outputFileName + "_RGB", bi, true, lossLessQuality);
        write(outputFileName + "_RGB", bi, false, lossLessQuality);
        write(outputFileName + "_RGB", bi, true, lossyQuality);
        write(outputFileName + "_RGB", bi, false, lossyQuality);
        write(outputFileName + "_JAI_RGB", bi, true, lossLessQuality, true);
        write(outputFileName + "_JAI_RGB", bi, false, lossLessQuality, true);
//        write(outputFileName + "_JAI_RGB", bi, true, lossyQuality, true);
//        write(outputFileName + "_JAI_RGB", bi, false, lossyQuality, true);
    }

    public void testPalettedRGB() throws IOException {
        BufferedImage bi = ImageIO.read(TestData.file(this, "paletted.tif"));
        write(outputFileName + "_RGB8", bi, true, lossLessQuality);
        write(outputFileName + "_RGB8", bi, false, lossLessQuality);
        write(outputFileName + "_JAI_RGB8", bi, true, lossLessQuality, true);
        write(outputFileName + "_JAI_RGB8", bi, false, lossLessQuality, true);
    }

    private static synchronized void write(String file, final RenderedImage bi,
            final boolean codeStreamOnly, final double quality)
            throws IOException {
        write(file, bi, codeStreamOnly, quality, false);
    }

    private static synchronized void write(String file, final RenderedImage bi,
            final boolean codeStreamOnly, final double quality,
            final boolean useJAI) throws IOException {
        write(file, bi, codeStreamOnly, quality, useJAI, null);
    }
}
