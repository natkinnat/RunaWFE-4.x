package ru.runa.wf.web.servlet;

import java.io.IOException;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.Part;

import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.FileUploadException;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.apache.commons.fileupload.servlet.ServletFileUpload;

import com.google.common.base.Charsets;
import com.google.common.io.ByteStreams;

public class MultipartRequestHandler {

//    public static List<UploadedFile> uploadByJavaServletAPI(HttpServletRequest request) throws IOException, ServletException {
//        List<UploadedFile> files = new LinkedList<UploadedFile>();
//        Collection<Part> parts = request.getParts();
//        UploadedFile temp = null;
//        for (Part part : parts) {
//            if (part.getContentType() != null) {
//                temp = new UploadedFile();
//                temp.setName(getFilename(part));
//                temp.setSize(part.getSize() / 1024 + " Kb");
//                temp.setMimeType(part.getContentType());
//                temp.setContent(part.getInputStream());
//                files.add(temp);
//            }
//        }
//        return files;
//    }

    public static String uploadByApacheFileUpload(HttpServletRequest request, UploadedFile file) throws IOException {
        boolean isMultipart = ServletFileUpload.isMultipartContent(request);
        String inputId = "";
        if (isMultipart) {
            DiskFileItemFactory factory = new DiskFileItemFactory();
            ServletFileUpload upload = new ServletFileUpload(factory);
            try {
                List<FileItem> items = upload.parseRequest(request);
                for (FileItem item : items) {
                    if (item.isFormField()) {
                        if (item.getFieldName().equals("inputId")) {
                            inputId = item.getString(Charsets.UTF_8.name());
                        }
                    } else {
                        file.setName(item.getName());
                        file.setContent(ByteStreams.toByteArray(item.getInputStream()));
                        file.setMimeType(item.getContentType());
                        String size;
                        if (item.getSize() > 1024 * 1024) {
                            size = item.getSize() / (1024 * 1024) + " Mb";
                        } else {
                            size = item.getSize() / 1024 + " Kb";
                        }
                        file.setSize(size);
                    }
                }
            } catch (FileUploadException e) {
                throw new IOException(e);
            }
        }
        return inputId;
    }

    private static String getFilename(Part part) {
        for (String cd : part.getHeader("content-disposition").split(";")) {
            if (cd.trim().startsWith("filename")) {
                String filename = cd.substring(cd.indexOf('=') + 1).trim().replace("\"", "");
                //     MSIE fix.
                return filename.substring(filename.lastIndexOf('/') + 1).substring(filename.lastIndexOf('\\') + 1); 
            }
        }
        return null;
    }
}