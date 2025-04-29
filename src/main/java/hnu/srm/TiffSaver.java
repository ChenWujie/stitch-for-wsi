package hnu.srm;

import com.twelvemonkeys.imageio.plugins.tiff.TIFFImageWriteParam;
import org.opencv.core.*;
import org.opencv.imgcodecs.Imgcodecs;

import javax.imageio.*;
import javax.imageio.stream.ImageOutputStream;
import java.awt.image.*;
import java.io.File;
import java.io.IOException;
import java.util.Iterator;

public class TiffSaver {

    public static void saveMatInRowChunks(Mat mat, File file, int chunkHeight) throws IOException {
        if (mat.empty()) {
            throw new IllegalArgumentException("Input Mat is empty.");
        }

        Iterator<ImageWriter> writers = ImageIO.getImageWritersByFormatName("tiff");
        if (!writers.hasNext()) {
            throw new IllegalStateException("No TIFF ImageWriter found. Make sure TwelveMonkeys TIFF plugin is in the classpath.");
        }

        ImageWriter writer = writers.next();
        ImageOutputStream ios = ImageIO.createImageOutputStream(file);
        writer.setOutput(ios);
        writer.prepareWriteSequence(null);

        int totalHeight = mat.rows();
        int width = mat.cols();

        for (int y = 0; y < totalHeight; y += chunkHeight) {
            int h = Math.min(chunkHeight, totalHeight - y);
            Rect roi = new Rect(0, y, width, h);
            Mat chunk = new Mat(mat, roi);

            BufferedImage chunkImage = matToBufferedImage(chunk);

            // 获取默认 WriteParam（TIFFImageWriteParam）
            ImageWriteParam param = writer.getDefaultWriteParam();
            if (param.canWriteCompressed()) {
                param.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
                param.setCompressionType("LZW");  // 可选: LZW, Deflate, JPEG, etc.
            }

            IIOImage iioImage = new IIOImage(chunkImage, null, null);
            writer.writeToSequence(iioImage, param);
        }

        writer.endWriteSequence();
        ios.close();
        writer.dispose();
    }



    private static BufferedImage matToBufferedImage(Mat mat) {
        if (!mat.isContinuous()) {
            mat = mat.clone(); // 确保连续
        }

        int width = mat.cols();
        int height = mat.rows();
        int channels = mat.channels();

        long bufferSize = (long) channels * width * height;
        if (bufferSize > Integer.MAX_VALUE) {
            throw new IllegalArgumentException("Mat too large to convert to BufferedImage: " + bufferSize);
        }

        int type;
        if (channels == 1) {
            type = BufferedImage.TYPE_BYTE_GRAY;
        } else if (channels == 3) {
            type = BufferedImage.TYPE_3BYTE_BGR;
        } else {
            throw new IllegalArgumentException("Unsupported number of channels: " + channels);
        }

        byte[] buffer = new byte[(int) bufferSize];
        mat.get(0, 0, buffer);

        BufferedImage image = new BufferedImage(width, height, type);
        byte[] targetPixels = ((DataBufferByte) image.getRaster().getDataBuffer()).getData();
        System.arraycopy(buffer, 0, targetPixels, 0, buffer.length);
        return image;
    }


}

