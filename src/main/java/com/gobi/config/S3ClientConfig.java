package com.gobi.config;
import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.ListObjectsV2Request;
import com.amazonaws.services.s3.model.ListObjectsV2Result;
import com.amazonaws.services.s3.model.S3ObjectSummary;
import com.gobi.controller.PayrollExcelController;
import com.gobi.controller.PlmController;
import com.gobi.controller.PlmTransferLogController;
import com.gobi.model.PlmTransferLog;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import java.util.Calendar;
import java.util.Date;

@Configuration
@EnableScheduling
public class S3ClientConfig {

    @Value("${aws.accessKey}")
    private String accessKey;
    @Value("${aws.secretKey}")
    private String secretKey;
    @Value("${aws.s3.bucket}")
    private String bucketName;
    @Value("${filePathForUploadedPayroll}")
    String uploadedPayrollPath;

    @Autowired
    PlmController plmController;
    @Autowired
    PayrollExcelController payrollExcelController;
    @Autowired
    PlmTransferLogController plmTransferLogController;

    @Bean
    public AmazonS3 initS3Client(){
        AWSCredentials credentials = new BasicAWSCredentials(this.accessKey, this.secretKey);
        return AmazonS3ClientBuilder.standard()
                .withRegion(Regions.AP_EAST_1)
                .withCredentials(new AWSStaticCredentialsProvider(credentials))
                .build();
    }

    @Scheduled(initialDelay = 1000, fixedDelay = 60000)
    public void checkEveryMinute(){
        System.out.println("checking... ");
        Calendar cal1 = Calendar.getInstance();
        cal1.add(Calendar.DAY_OF_MONTH, -1);
        Date yesterday = cal1.getTime();
        Date firstRangeOfTime = plmTransferLogController.getLastSuccessLogDate();
        Calendar cal2 = Calendar.getInstance();
        Date secondRangeOfTime = cal2.getTime();

        final ListObjectsV2Request req = new ListObjectsV2Request().withBucketName(bucketName);
        ListObjectsV2Result result;
        do {
            AmazonS3 s3Client = initS3Client();
            result = s3Client.listObjectsV2(req);
            String newAddedFileName;
            for (S3ObjectSummary objectSummary : result.getObjectSummaries()) {
                if(objectSummary.getLastModified().before(secondRangeOfTime) && objectSummary.getLastModified().after(firstRangeOfTime)){
                    newAddedFileName = objectSummary.getKey();
                    if(!newAddedFileName.equals("")){
                        if(newAddedFileName.startsWith("routing")){
//                            Boolean routingResult = routingController.postRoutingByFileName(newAddedFileName);
                        } else if (newAddedFileName.startsWith("bom")) {
//                            Boolean bomResult = bomController.postBomByFileName(newAddedFileName);
                        } else {
                            ResponseEntity<?> responseEntity = plmController.save(newAddedFileName);
                            PlmTransferLog plmTransferLog = new PlmTransferLog();
                            plmTransferLog.setTransferDate(objectSummary.getLastModified());
                            plmTransferLog.setFileName(objectSummary.getKey());
                            plmTransferLog.setIsSuccessful(responseEntity.getStatusCode().value() == 200);
                            plmTransferLog.setIsRetransfer(objectSummary.getLastModified().before(yesterday));
                            plmTransferLog.setResponseMessage(responseEntity.getStatusCode().toString());
                            plmTransferLogController.post(plmTransferLog);
                        }
                    }
                }
            }
            req.setContinuationToken(result.getNextContinuationToken());
        } while(result.isTruncated());
    }
}
