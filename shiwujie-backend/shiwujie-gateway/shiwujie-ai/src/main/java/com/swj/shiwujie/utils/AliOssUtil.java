//package com.swj.shiwujie.utils;
//
//import com.aliyun.oss.ClientException;
//import com.aliyun.oss.OSS;
//import com.aliyun.oss.OSSClientBuilder;
//import com.aliyun.oss.OSSException;
//import com.aliyun.oss.model.ObjectMetadata;
//import com.aliyun.oss.model.PutObjectRequest;
//import com.aliyun.oss.model.PutObjectResult;
//import lombok.extern.slf4j.Slf4j;
//
//import java.io.InputStream;
//
//@Slf4j
//public class AliOssUtil {
//    // Endpoint以华东1（成都）为例
//    private static final String ENDPOINT = "https://oss-cn-chengdu.aliyuncs.com";
//    private static final String ACCESS_KEY_ID="LTAI5tMEro4Cf3LVQweNHRu5";
//    private static final String ACCESS_KEY_SECRET="QEtaiV2B6KbJ7QKDt851faYxPtYdGO";
//    // 填写Bucket名称
//    private static final String BUCKET_NAME = "shiwujie";
//
//
//    /**
//     *
//     * @param objectName
//     * @param in
//     * @return  文件上传成功，返回访问地址
//     * @throws Exception
//     */
//    public static String uploadFile(String objectName, InputStream in) throws Exception {
//        // 创建OSSClient实例。
//        OSS ossClient = new OSSClientBuilder().build(ENDPOINT, ACCESS_KEY_ID, ACCESS_KEY_SECRET);
//        try {
//            // 创建PutObjectRequest对象。
//            PutObjectRequest putObjectRequest = new PutObjectRequest(BUCKET_NAME, objectName, in);
//
//            // 设置文件的元数据
//            ObjectMetadata metadata = new ObjectMetadata();
//            metadata.setContentType(getContentType(objectName));
//            // 设置权限为公共读
//            metadata.setObjectAcl(com.aliyun.oss.model.CannedAccessControlList.PublicRead);
//            putObjectRequest.setMetadata(metadata);
//
//            // 上传文件。
//            PutObjectResult result = ossClient.putObject(putObjectRequest);
//
//            // 检查上传结果
//            if (result.getResponse() != null && (result.getResponse().getStatusCode() != 200 && result.getResponse().getStatusCode() != 204)) {
//                throw new RuntimeException("文件上传失败，HTTP状态码: " + result.getResponse().getStatusCode());
//            }
//
//            // url组成:https://bucket名称.区域节点/objectName
//            // 需要移除ENDPOINT中的协议部分(https://)
//            String endpointHost = ENDPOINT.replace("https://", "").replace("http://", "");
//            String url = "https://" + BUCKET_NAME + "." + endpointHost + "/" + objectName;
//
//            log.info("文件上传成功，访问地址: {}", url);
//            return url;
//        } catch (OSSException oe) {
//            log.error("OSS异常，请求被OSS接收但被拒绝，错误信息: {}", oe.getErrorMessage());
//            log.error("错误代码: {}, 请求ID: {}, Host ID: {}", oe.getErrorCode(), oe.getRequestId(), oe.getHostId());
//            throw new RuntimeException("文件上传失败: " + oe.getErrorMessage(), oe);
//        } catch (ClientException ce) {
//            log.error("客户端异常，连接OSS时发生严重内部问题，错误信息: {}", ce.getMessage());
//            throw new RuntimeException("文件上传失败: " + ce.getMessage(), ce);
//        } catch (Exception e) {
//            log.error("文件上传过程中发生未知异常", e);
//            throw new RuntimeException("文件上传失败: " + e.getMessage(), e);
//        } finally {
//            if (ossClient != null) {
//                ossClient.shutdown();
//            }
//        }
//    }
//
//    /**
//     * 根据文件名获取Content-Type
//     * @param fileName 文件名
//     * @return Content-Type
//     */
//    private static String getContentType(String fileName) {
//        if (fileName == null) {
//            return "application/octet-stream";
//        }
//
//        String lowerFileName = fileName.toLowerCase();
//        if (lowerFileName.endsWith(".png")) {
//            return "image/png";
//        } else if (lowerFileName.endsWith(".jpg") || lowerFileName.endsWith(".jpeg")) {
//            return "image/jpeg";
//        } else if (lowerFileName.endsWith(".gif")) {
//            return "image/gif";
//        } else if (lowerFileName.endsWith(".bmp")) {
//            return "image/bmp";
//        } else {
//            return "application/octet-stream";
//        }
//    }
//}