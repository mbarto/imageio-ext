/*
 *    ImageI/O-Ext - OpenSource Java Image translation Library
 *    http://www.geo-solutions.it/
 *    https://imageio-ext.dev.java.net/
 *    (C) 2007 - 2008, GeoSolutions
 *
 *    This library is free software; you can redistribute it and/or
 *    modify it under the terms of the GNU Lesser General Public
 *    License as published by the Free Software Foundation;
 *    version 2.1 of the License.
 *
 *    This library is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *    Lesser General Public License for more details.
 */
package it.geosolutions.imageio.plugins.geotiff;

import it.geosolutions.imageio.gdalframework.AbstractGDALTest;
import it.geosolutions.imageio.gdalframework.Viewer;
import it.geosolutions.imageio.stream.input.FileImageInputStreamExtImpl;
import it.geosolutions.resources.TestData;

import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.image.RenderedImage;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Locale;

import javax.imageio.ImageReadParam;
import javax.imageio.ImageReader;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.metadata.IIOMetadata;
import javax.media.jai.ImageLayout;
import javax.media.jai.JAI;
import javax.media.jai.ParameterBlockJAI;
import javax.media.jai.RenderedOp;

import org.junit.Assert;

import com.sun.media.jai.operator.ImageWriteDescriptor;

/**
 * @author Daniele Romagnoli, GeoSolutions.
 * @author Simone Giannecchini, GeoSolutions.
 */
public class GeoTiffTest extends AbstractGDALTest {

    public GeoTiffTest() {
        super();
    }

    /**
     * Test Read without exploiting JAI-ImageIO Tools
     * 
     * @throws FileNotFoundException
     * @throws IOException
     */
    @org.junit.Test
    public void manualRead() throws IOException, FileNotFoundException {
        if (!isGDALAvailable) {
            return;
        }
        final ImageReadParam irp = new ImageReadParam();

        // Reading a simple GrayScale image
        String fileName = "utm.tif";
        final File inputFile = TestData.file(this, fileName);
        irp.setSourceSubsampling(2, 2, 0, 0);
        ImageReader reader = new GeoTiffImageReaderSpi().createReaderInstance();
        reader.setInput(inputFile);
        final RenderedImage image = reader.readAsRenderedImage(0, irp);
        if (TestData.isInteractiveTest())
            Viewer.visualizeAllInformation(image, fileName);
        Assert.assertEquals(256, image.getWidth());
        Assert.assertEquals(256, image.getHeight());
        reader.dispose();
    }

    /**
     * Test Read exploiting JAI-ImageIO tools capabilities
     * 
     * @throws FileNotFoundException
     * @throws IOException
     */
    @org.junit.Test
    public void read() throws FileNotFoundException, IOException {
        if (!isGDALAvailable) {
            return;
        }
        final ParameterBlockJAI pbjImageRead;
        String fileName = "utm.tif";
        final File file = TestData.file(this, fileName);

        pbjImageRead = new ParameterBlockJAI("ImageRead");
        pbjImageRead.setParameter("Input", new FileImageInputStreamExtImpl(file));
        pbjImageRead.setParameter("Reader", new GeoTiffImageReaderSpi().createReaderInstance());
        RenderedOp image = JAI.create("ImageRead", pbjImageRead);
        if (TestData.isInteractiveTest())
            Viewer.visualizeAllInformation(image, "", true);
        else
        	Assert.assertNotNull(image.getTiles());
    }

    /**
     * Test Writing capabilities.
     * 
     * @throws FileNotFoundException
     * @throws IOException
     */
    @org.junit.Test
    public void write() throws IOException, FileNotFoundException {
        if (!isGDALAvailable) {
            return;
        }
        final File outputFile = TestData.temp(this, "writetest.tif", false);
        outputFile.deleteOnExit();
        final File inputFile = TestData.file(this, "utm.tif");

        ImageReadParam rparam = new ImageReadParam();
        rparam.setSourceRegion(new Rectangle(1, 1, 300, 500));
        rparam.setSourceSubsampling(1, 2, 0, 0);
        ImageReader reader = new GeoTiffImageReaderSpi().createReaderInstance();
        reader.setInput(inputFile);
        final IIOMetadata metadata = reader.getImageMetadata(0);

        final ParameterBlockJAI pbjImageRead = new ParameterBlockJAI("ImageRead");
        pbjImageRead.setParameter("Input", inputFile);
        pbjImageRead.setParameter("reader", reader);
        pbjImageRead.setParameter("readParam", rparam);

        final ImageLayout l = new ImageLayout();
        l.setTileGridXOffset(0).setTileGridYOffset(0).setTileHeight(256).setTileWidth(256);

        RenderedOp image = JAI.create("ImageRead", pbjImageRead,new RenderingHints(JAI.KEY_IMAGE_LAYOUT, l));

        if (TestData.isInteractiveTest())
            Viewer.visualizeAllInformation(image,"geotiff");

        // ////////////////////////////////////////////////////////////////
        // preparing to write
        // ////////////////////////////////////////////////////////////////
        final ParameterBlockJAI pbjImageWrite = new ParameterBlockJAI("ImageWrite");
        ImageWriter writer = new GeoTiffImageWriterSpi().createWriterInstance();
        pbjImageWrite.setParameter("Output", outputFile);
        pbjImageWrite.setParameter("writer", writer);
        pbjImageWrite.setParameter("ImageMetadata", metadata);
        pbjImageWrite.setParameter("Transcode", false);
        ImageWriteParam param = new ImageWriteParam(Locale.getDefault());
        param.setSourceRegion(new Rectangle(10, 10, 100, 100));
        param.setSourceSubsampling(2, 1, 0, 0);
        pbjImageWrite.setParameter("writeParam", param);

        pbjImageWrite.addSource(image);
        final RenderedOp op = JAI.create("ImageWrite", pbjImageWrite);
        final ImageWriter writer2 = (ImageWriter) op.getProperty(ImageWriteDescriptor.PROPERTY_NAME_IMAGE_WRITER);
        writer2.dispose();

        // ////////////////////////////////////////////////////////////////
        // preparing to read again
        // ////////////////////////////////////////////////////////////////
        final ParameterBlockJAI pbjImageReRead = new ParameterBlockJAI("ImageRead");
        pbjImageReRead.setParameter("Input", outputFile);
        pbjImageReRead.setParameter("Reader", new GeoTiffImageReaderSpi() .createReaderInstance());
        final RenderedOp image2 = JAI.create("ImageRead", pbjImageReRead);
        if (TestData.isInteractiveTest())
            Viewer.visualizeAllInformation(image2,"geotif2");
        else
        	Assert.assertNotNull(image2.getTiles());
    }

    /**
     * Test Read on a Paletted Image
     * 
     * @throws FileNotFoundException
     * @throws IOException
     */
    @org.junit.Test
    public void palette() throws FileNotFoundException, IOException {
        if (!isGDALAvailable) {
            return;
        }
        final File outputFile = TestData.temp(this, "writetest.tif", false);
        outputFile.deleteOnExit();
        final File inputFile = TestData.file(this, "paletted.tif");

        ImageReader reader = new GeoTiffImageReaderSpi().createReaderInstance();
        reader.setInput(inputFile);
        final IIOMetadata metadata = reader.getImageMetadata(0);

        final ParameterBlockJAI pbjImageRead = new ParameterBlockJAI("ImageRead");
        pbjImageRead.setParameter("Input", inputFile);
        pbjImageRead.setParameter("reader", reader);

        final ImageLayout l = new ImageLayout();
        l.setTileGridXOffset(0).setTileGridYOffset(0).setTileHeight(256).setTileWidth(256);

        RenderedOp image = JAI.create("ImageRead", pbjImageRead, new RenderingHints(JAI.KEY_IMAGE_LAYOUT, l));

        if (TestData.isInteractiveTest())
            Viewer.visualizeAllInformation(image, "Paletted image read");

        // ////////////////////////////////////////////////////////////////
        // preparing to write
        // ////////////////////////////////////////////////////////////////
        final ParameterBlockJAI pbjImageWrite = new ParameterBlockJAI("ImageWrite");
        ImageWriter writer = new GeoTiffImageWriterSpi().createWriterInstance();
        pbjImageWrite.setParameter("Output", outputFile);
        pbjImageWrite.setParameter("writer", writer);
        pbjImageWrite.setParameter("ImageMetadata", metadata);
        pbjImageWrite.setParameter("Transcode", false);
        pbjImageWrite.addSource(image);
        final RenderedOp op = JAI.create("ImageWrite", pbjImageWrite);
        final ImageWriter writer2 = (ImageWriter) op.getProperty(ImageWriteDescriptor.PROPERTY_NAME_IMAGE_WRITER);
        writer2.dispose();

        // ////////////////////////////////////////////////////////////////
        // preparing to read again
        // ////////////////////////////////////////////////////////////////
        final ParameterBlockJAI pbjImageReRead = new ParameterBlockJAI("ImageRead");
        pbjImageReRead.setParameter("Input", outputFile);
        pbjImageReRead.setParameter("Reader", new GeoTiffImageReaderSpi().createReaderInstance());
        final RenderedOp image2 = JAI.create("ImageRead", pbjImageReRead);
        if (TestData.isInteractiveTest())
            Viewer.visualizeAllInformation(image2,"Paletted image read back after writing");
        else
        	Assert.assertNotNull(image2.getTiles());
    }
}