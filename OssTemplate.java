package com.atguigu.utils;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.UUID;

import org.apache.commons.lang.time.DateUtils;

import com.aliyun.oss.OSSClient;
import com.aliyun.oss.model.PutObjectResult;

import lombok.Data;
import lombok.ToString;


@ToString
@Data
public class OssTemplate {
	
	
	private String endpoint;
	private String bucketDomain;//atcrowdfunding.oss-cn-beijing.aliyuncs.com
	private String accessKeyId;
	private String accessKeySecret;
	private String bucketName;
	
	
	/**
	 * 返回上传后的文件的访问路径
	 * @param inputStream
	 * @param fileName
	 * @return
	 * @throws IOException 
	 */
	public String upload(InputStream inputStream,String fileName) throws IOException{
		
				//加工文件夹和文件名
				String folderName = AppDateUtils.getFormatTime("yyyy-MM-dd");
				fileName = UUID.randomUUID().toString().replace("-", "")+"_"+fileName;
				
				// Endpoint以杭州为例，其它Region请按实际情况填写。
				// 云账号AccessKey有所有API访问权限，建议遵循阿里云安全最佳实践，创建并使用RAM子账号进行API访问或日常运维，请登录 https://ram.console.aliyun.com 创建。
				//LTAIZgCN8dG8nxSZ
				//gGFurxBCjpKhNIrD5qorFiCg0MmKUj
				// 创建OSSClient实例。
				OSSClient ossClient = new OSSClient(endpoint, accessKeyId, accessKeySecret);
				// 上传文件流。
				// 指定bucket的名
				ossClient.putObject(bucketName, "pic/"+folderName+"/"+fileName, inputStream);
				inputStream.close();
				ossClient.shutdown();
				
				return bucketDomain+"/pic/"+folderName+"/"+fileName;
	}

}
