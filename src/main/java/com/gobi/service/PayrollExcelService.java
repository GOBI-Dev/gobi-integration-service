package com.gobi.service;

import com.gobi.dto.FileClientDTO;
import com.gobi.model.PayrollExcel;
import com.gobi.repository.PayrollExcelRepository;
import com.gobi.util.Util;
import net.minidev.json.JSONObject;
import net.minidev.json.parser.JSONParser;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;
import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

@Service
public class PayrollExcelService {

    @Autowired
    PayrollExcelRepository payrollExcelRepository;

    @Autowired
    PayrollReferenceService payrollReferenceService;

    @Value("${filePathForPayroll}")
    String filePath;

    public Page<PayrollExcel> findAll(Specification<PayrollExcel> specs, Pageable pageable) {
        return payrollExcelRepository.findAll(Specification.where(specs), pageable);
    }
    private String getCellValue(Row row, int cellNo) {
        DataFormatter formatter = new DataFormatter();
        Cell cell = row.getCell(cellNo);
        return formatter.formatCellValue(cell);
    }

    public ResponseEntity<String> postPayrollExcelMultipart(MultipartFile multipartFile) throws ParseException, InterruptedException {
        List<PayrollExcel> result = new ArrayList<>();
        FileClientDTO fileClientDTO = new FileClientDTO();
        fileClientDTO.setUuid(UUID.randomUUID().toString());
        String filename = multipartFile.getOriginalFilename();
        fileClientDTO.setFormat(Objects.requireNonNull(filename).substring(filename.lastIndexOf(".")));
        Long periodInExcel=null, yearInExcel = null;
        try {
            byte[] bytes = multipartFile.getBytes();
            Path path = Paths.get(filePath + fileClientDTO.getUuid() + fileClientDTO.getFormat());
            Files.write(path, bytes);
        } catch (Exception e) {
            System.out.println("e: "+e);
        }

        XSSFWorkbook workbook = null;
        try {
            workbook = new XSSFWorkbook(filePath + fileClientDTO.getUuid() + fileClientDTO.getFormat());
        } catch (IOException e) {
            e.printStackTrace();
        }

        XSSFSheet worksheet1 = workbook != null ? workbook.getSheet("P1") : null;
        XSSFSheet worksheet7 = workbook != null ? workbook.getSheet("P7") : null;

        String pattern = "yyyy-MM-dd";
        DateFormat df = new SimpleDateFormat(pattern);
        String timePattern = "HH:mm:ss";
        DateFormat dft = new SimpleDateFormat(timePattern);
        Date today = new Date();
        String zDate = df.format(today);
        String zTime = dft.format(today);
        zTime = zTime.replace(":", "");
        String patternZHID = "yyMM";
        DateFormat zhid = new SimpleDateFormat(patternZHID);
        String subZHID = zhid.format(today);

        Date nowDate = Calendar.getInstance().getTime();
        String nowDateString = new SimpleDateFormat("yyyy-MM-dd").format(nowDate);
        Date startDate = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").parse(nowDateString + " 00:00:00");
        Date endDate = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").parse(nowDateString + " 23:59:59");
        int sapIndex=0;

        List<Long> lastSerialNos = payrollExcelRepository.getSerialNosByYearAndPeriod(yearInExcel, periodInExcel);
        long lastSerial;
        if(lastSerialNos.size()>0){
            lastSerial = lastSerialNos.get(0);
            for(Long lastSerialOne : lastSerialNos){
                if(lastSerialOne>lastSerial){
                    lastSerial = lastSerialOne;
                }
            }
        }else{
            lastSerial = 1L;
        }

        StringBuilder itemList = new StringBuilder();
        StringBuilder itemList1 = new StringBuilder();
        StringBuilder itemList2 = new StringBuilder();
        StringBuilder itemList3 = new StringBuilder();
        StringBuilder itemList4 = new StringBuilder();
        StringBuilder itemList5 = new StringBuilder();
        StringBuilder itemList6 = new StringBuilder();

        //COSTCENTER: herev excel-d sheet ner n P1 baival ugugdul gej oilgon avah.
        if(worksheet1!=null){
            //local-d ugugduluudiig hadgalj bn.
            String cellValue1 = getCellValue(worksheet1.getRow(5), 2);
            List<String> costCenterList = new ArrayList<>();
            int index=4;

            List<String> duplicationCostCenter = new ArrayList<>();
            while(cellValue1!=null){
                if (!getCellValue(worksheet1.getRow(index), 2).equals("")) {

                    if(!duplicationCostCenter.contains(getCellValue(worksheet1.getRow(index), 6))){
                        PayrollExcel payrollExcel = new PayrollExcel();
                        payrollExcel.setYear(Long.valueOf(getCellValue(worksheet1.getRow(index), 2)));
                        payrollExcel.setPeriod(Long.valueOf(getCellValue(worksheet1.getRow(index), 3)));
                        lastSerialNos = payrollExcelRepository.getSerialNosByYearAndPeriod(payrollExcel.getYear(), payrollExcel.getPeriod());

                        long lastSerialNo;
                        if(lastSerialNos.size()>0){
                            lastSerialNo = lastSerialNos.get(0)+1;
                        }else {lastSerialNo = 1L;}

                        payrollExcel.setSerialNo(lastSerialNo);
                        payrollExcel.setCostCenter(getCellValue(worksheet1.getRow(index), 6));
                        if (!costCenterList.contains(payrollExcel.getCostCenter())) {
                            costCenterList.add(payrollExcel.getCostCenter());
                        }
                        if (!getCellValue(worksheet1.getRow(index), 8).equals("") && isNumeric(getCellValue(worksheet1.getRow(index), 8))) {
                            payrollExcel.setEmployeeWages(new BigDecimal(getCellValue(worksheet1.getRow(index), 8)));
                        }
                        if (!getCellValue(worksheet1.getRow(index), 9).equals("") && isNumeric(getCellValue(worksheet1.getRow(index), 9))) {
                            payrollExcel.setSocialInsuranceFromEmployee(new BigDecimal(getCellValue(worksheet1.getRow(index), 9)));
                        }
                        if (!getCellValue(worksheet1.getRow(index), 10).equals("") && isNumeric(getCellValue(worksheet1.getRow(index), 10))) {
                            payrollExcel.setSocialInsuranceFromCompany(new BigDecimal(getCellValue(worksheet1.getRow(index), 10)));
                        }
                        if (!getCellValue(worksheet1.getRow(index), 11).equals("") && isNumeric(getCellValue(worksheet1.getRow(index), 11))) {
                            payrollExcel.setPersonalIncomeTax(new BigDecimal(getCellValue(worksheet1.getRow(index), 11)));
                        }
                        if (!getCellValue(worksheet1.getRow(index), 12).equals("") && isNumeric(getCellValue(worksheet1.getRow(index), 12))) {
                            payrollExcel.setAdvancePayroll(new BigDecimal(getCellValue(worksheet1.getRow(index), 12)));
                        }
                        if (!getCellValue(worksheet1.getRow(index), 14).equals("") && isNumeric(getCellValue(worksheet1.getRow(index), 14))) {
                            payrollExcel.setTradeUnionFee(new BigDecimal(getCellValue(worksheet1.getRow(index), 14)));
                        }

                        payrollExcel.setStatus(true);
                        payrollExcel.setIsLog(false);
                        payrollExcel.setCreatedDate(new Date());
                        result.add(payrollExcel);
                        duplicationCostCenter.add(getCellValue(worksheet1.getRow(index), 6));
                    }
                }else {
                    cellValue1 = null;
                }
                index++;
            }
            System.out.println("result size: "+result.size());
            payrollExcelRepository.saveAll(result);
            result = new ArrayList<>();
//            Thread.sleep(5 * 1000);

            //local-s suuld tatagdsan ugugdluudiig tsugluulan avch bolovsruulj bn.
            lastSerialNos = payrollExcelRepository.getSerialNosByYearAndPeriod(yearInExcel, periodInExcel);
            if(lastSerialNos.size()>0){
                lastSerial = lastSerialNos.get(0);
                for(Long lastSerialOne : lastSerialNos){
                    if(lastSerialOne>lastSerial){
                        lastSerial = lastSerialOne;
                    }
                }
            }else{
                lastSerial = 1L;
            }
            if(lastSerial==1){
                lastSerial=2;
            }
            BigDecimal hulDunEmployeeWage = BigDecimal.valueOf(payrollExcelRepository.hulDunEmployeeWages(lastSerial-1));
            BigDecimal hulDunEmployeeInsurance = BigDecimal.valueOf(payrollExcelRepository.hulDunSocialInsuranceFromEmployee(lastSerial-1));
            BigDecimal hulDunCompanyInsurance = BigDecimal.valueOf(payrollExcelRepository.hulDunSocialInsuranceFromCompany(lastSerial-1));
            BigDecimal hulDunTax = BigDecimal.valueOf(payrollExcelRepository.hulDunIncomeTax(lastSerial-1));
            BigDecimal hulDunAdvancePayroll = BigDecimal.valueOf(payrollExcelRepository.hulDunAdvancePayroll(lastSerial-1));
            BigDecimal hulDunTradeUnionFee = BigDecimal.valueOf(payrollExcelRepository.hulDunTradeUnionFee(lastSerial-1));

            costCenterList = payrollExcelRepository.getCostCenters(lastSerial-1);
            System.out.println("costCenterList: "+costCenterList);
            String stringJson1, stringJson2, stringJson3, stringJson4, stringJson5, stringJson6;
            for(String costCenter : costCenterList){
//                    BigDecimal totalEmployeeWage = BigDecimal.valueOf(Long.parseLong(String.valueOf(payrollExcelRepository.totalEmployeeWages(costCenter, startDate, endDate, lastSerial))));
//                    BigDecimal totalEmployeeInsurance = BigDecimal.valueOf(Long.parseLong(String.valueOf(payrollExcelRepository.totalSocialInsuranceFromEmployee(costCenter, startDate, endDate, lastSerial))));
//                    BigDecimal totalCompanyInsurance = BigDecimal.valueOf(Long.parseLong(String.valueOf(payrollExcelRepository.totalSocialInsuranceFromCompany(costCenter, startDate, endDate, lastSerial))));
//                    BigDecimal totalTax = BigDecimal.valueOf(Long.parseLong(String.valueOf(payrollExcelRepository.totalPersonalIncomeTax(costCenter, startDate, endDate, lastSerial))));
//                    BigDecimal totalAdvancePayroll = BigDecimal.valueOf(Long.parseLong(String.valueOf(payrollExcelRepository.totalAdvancePayroll(costCenter, startDate, endDate, lastSerial))));
//                    BigDecimal totalTradeUnionFee = BigDecimal.valueOf(Long.parseLong(String.valueOf(payrollExcelRepository.totalTradeUnionFee(costCenter, startDate, endDate, lastSerial))));
                PayrollExcel payrollExcel = payrollExcelRepository.getByCostCenterAndSerialNo(costCenter, lastSerial-1).get(0);

                {
                    List<JSONObject> map = new LinkedList<>();
                    {JSONObject object = new JSONObject();
                        object.put("Zhid", "P01"+subZHID+lastSerial);
                        map.add(object);}
                    sapIndex++;
                    {JSONObject object = new JSONObject();
                        object.put("Zinu", sapIndex);
                        map.add(object);}
                    {JSONObject object = new JSONObject();
                        object.put("Gjahr", payrollExcel.getYear());
                        map.add(object);}
                    {JSONObject object = new JSONObject();
                        object.put("Monat", payrollExcel.getPeriod());
                        map.add(object);}
                    {JSONObject object = new JSONObject();
                        object.put("Bukrs", 8000);
                        map.add(object);}
                    {JSONObject object = new JSONObject();
                        object.put("Waers", "MNT");
                        map.add(object);}
                    {JSONObject object = new JSONObject();
                        object.put("Zbty", "P01");
                        map.add(object);}
                    {JSONObject object = new JSONObject();
                        object.put("Hsl", payrollExcel.getEmployeeWages());
                        map.add(object);}
                    {JSONObject object = new JSONObject();
                        object.put("Rcntr", costCenter);
                        map.add(object);}
                    {JSONObject object = new JSONObject();
                        object.put("Kunnr", "");
                        map.add(object);}
                    {JSONObject object = new JSONObject();
                        object.put("Ksl", hulDunEmployeeWage);
                        map.add(object);}

                    stringJson1 = map.toString();
                    stringJson1 = stringJson1.replace("{", "");
                    stringJson1 = stringJson1.replace("}", "");
                    stringJson1 = stringJson1.replace("]", "}");
                    stringJson1 = stringJson1.replace("[", "{");
                    itemList1.append(", ").append(stringJson1);
                }
                {
                    List<JSONObject> map = new LinkedList<>();
                    {JSONObject object = new JSONObject();

                        object.put("Zhid", "P02"+subZHID+lastSerial);
                        map.add(object);}
                    sapIndex++;
                    {JSONObject object = new JSONObject();
                        object.put("Zinu", sapIndex);
                        map.add(object);}
                    {JSONObject object = new JSONObject();
                        object.put("Gjahr", payrollExcel.getYear());
                        map.add(object);}
                    {JSONObject object = new JSONObject();
                        object.put("Monat", payrollExcel.getPeriod());
                        map.add(object);}
                    {JSONObject object = new JSONObject();
                        object.put("Bukrs", 8000);
                        map.add(object);}
                    {JSONObject object = new JSONObject();
                        object.put("Waers", "MNT");
                        map.add(object);}
                    {JSONObject object = new JSONObject();
                        object.put("Zbty", "P02");
                        map.add(object);}
                    {JSONObject object = new JSONObject();
                        object.put("Hsl", payrollExcel.getSocialInsuranceFromCompany());
                        map.add(object);}
                    {JSONObject object = new JSONObject();
                        object.put("Rcntr", costCenter);
                        map.add(object);}
                    {JSONObject object = new JSONObject();
                        object.put("Kunnr", "");
                        map.add(object);}
                    {JSONObject object = new JSONObject();
                        object.put("Ksl", hulDunCompanyInsurance);
                        map.add(object);}
                    stringJson2 = map.toString();
                    stringJson2 = stringJson2.replace("{", "");
                    stringJson2 = stringJson2.replace("}", "");
                    stringJson2 = stringJson2.replace("]", "}");
                    stringJson2 = stringJson2.replace("[", "{");
                    itemList2.append(", ").append(stringJson2);
                }
                {
                    List<JSONObject> map = new LinkedList<>();
                    {JSONObject object = new JSONObject();
                        object.put("Zhid", "P03"+subZHID+lastSerial);
                        map.add(object);}
                    sapIndex++;
                    {JSONObject object = new JSONObject();
                        object.put("Zinu", sapIndex);
                        map.add(object);}
                    {JSONObject object = new JSONObject();
                        object.put("Gjahr", payrollExcel.getYear());
                        map.add(object);}
                    {JSONObject object = new JSONObject();
                        object.put("Monat", payrollExcel.getPeriod());
                        map.add(object);}
                    {JSONObject object = new JSONObject();
                        object.put("Bukrs", 8000);
                        map.add(object);}
                    {JSONObject object = new JSONObject();
                        object.put("Waers", "MNT");
                        map.add(object);}
                    {JSONObject object = new JSONObject();
                        object.put("Zbty", "P03");
                        map.add(object);}
                    {JSONObject object = new JSONObject();
                        object.put("Hsl", payrollExcel.getSocialInsuranceFromEmployee());
                        map.add(object);}
                    {JSONObject object = new JSONObject();
                        object.put("Rcntr", costCenter);
                        map.add(object);}
                    {JSONObject object = new JSONObject();
                        object.put("Kunnr", "");
                        map.add(object);}
                    {JSONObject object = new JSONObject();
                        object.put("Ksl", hulDunEmployeeInsurance);
                        map.add(object);}

                    stringJson3 = map.toString();
                    stringJson3 = stringJson3.replace("{", "");
                    stringJson3 = stringJson3.replace("}", "");
                    stringJson3 = stringJson3.replace("]", "}");
                    stringJson3 = stringJson3.replace("[", "{");
                    itemList3.append(", ").append(stringJson3);
                }
                {
                    List<JSONObject> map = new LinkedList<>();
                    {JSONObject object = new JSONObject();
                        object.put("Zhid", "P04"+subZHID+lastSerial);
                        map.add(object);}
                    sapIndex++;
                    {JSONObject object = new JSONObject();
                        object.put("Zinu", sapIndex);
                        map.add(object);}
                    {JSONObject object = new JSONObject();
                        object.put("Gjahr", payrollExcel.getYear());
                        map.add(object);}
                    {JSONObject object = new JSONObject();
                        object.put("Monat", payrollExcel.getPeriod());
                        map.add(object);}
                    {JSONObject object = new JSONObject();
                        object.put("Bukrs", 8000);
                        map.add(object);}
                    {JSONObject object = new JSONObject();
                        object.put("Waers", "MNT");
                        map.add(object);}
                    {JSONObject object = new JSONObject();
                        object.put("Zbty", "P04");
                        map.add(object);}
                    {JSONObject object = new JSONObject();
                        object.put("Hsl", payrollExcel.getPersonalIncomeTax());
                        map.add(object);}
                    {JSONObject object = new JSONObject();
                        object.put("Rcntr", costCenter);
                        map.add(object);}
                    {JSONObject object = new JSONObject();
                        object.put("Kunnr", "");
                        map.add(object);}
                    {JSONObject object = new JSONObject();
                        object.put("Ksl", hulDunTax);
                        map.add(object);}

                    stringJson4 = map.toString();
                    stringJson4 = stringJson4.replace("{", "");
                    stringJson4 = stringJson4.replace("}", "");
                    stringJson4 = stringJson4.replace("]", "}");
                    stringJson4 = stringJson4.replace("[", "{");
                    itemList4.append(", ").append(stringJson4);
                }
                {
                    List<JSONObject> map = new LinkedList<>();
                    {JSONObject object = new JSONObject();
                        object.put("Zhid", "P05"+subZHID+lastSerial);
                        map.add(object);}
                    sapIndex++;
                    {JSONObject object = new JSONObject();
                        object.put("Zinu", sapIndex);
                        map.add(object);}
                    {JSONObject object = new JSONObject();
                        object.put("Gjahr", payrollExcel.getYear());
                        map.add(object);}
                    {JSONObject object = new JSONObject();
                        object.put("Monat", payrollExcel.getPeriod());
                        map.add(object);}
                    {JSONObject object = new JSONObject();
                        object.put("Bukrs", 8000);
                        map.add(object);}
                    {JSONObject object = new JSONObject();
                        object.put("Waers", "MNT");
                        map.add(object);}
                    {JSONObject object = new JSONObject();
                        object.put("Zbty", "P05");
                        map.add(object);}
                    {JSONObject object = new JSONObject();
                        object.put("Hsl", payrollExcel.getTradeUnionFee());
                        map.add(object);}
                    {JSONObject object = new JSONObject();
                        object.put("Rcntr", costCenter);
                        map.add(object);}
                    {JSONObject object = new JSONObject();
                        object.put("Kunnr", "");
                        map.add(object);}
                    {JSONObject object = new JSONObject();
                        object.put("Ksl", hulDunTradeUnionFee);
                        map.add(object);}

                    stringJson5 = map.toString();
                    stringJson5 = stringJson5.replace("{", "");
                    stringJson5 = stringJson5.replace("}", "");
                    stringJson5 = stringJson5.replace("]", "}");
                    stringJson5 = stringJson5.replace("[", "{");
                    itemList5.append(", ").append(stringJson5);
                }
                {
                    List<JSONObject> map = new LinkedList<>();
                    {JSONObject object = new JSONObject();
                        object.put("Zhid", "P06"+subZHID+lastSerial);
                        map.add(object);}
                    sapIndex++;
                    {JSONObject object = new JSONObject();
                        object.put("Zinu", sapIndex);
                        map.add(object);}
                    {JSONObject object = new JSONObject();
                        object.put("Gjahr", payrollExcel.getYear());
                        map.add(object);}
                    {JSONObject object = new JSONObject();
                        object.put("Monat", payrollExcel.getPeriod());
                        map.add(object);}
                    {JSONObject object = new JSONObject();
                        object.put("Bukrs", 8000);
                        map.add(object);}
                    {JSONObject object = new JSONObject();
                        object.put("Waers", "MNT");
                        map.add(object);}
                    {JSONObject object = new JSONObject();
                        object.put("Zbty", "P06");
                        map.add(object);}
                    {JSONObject object = new JSONObject();
                        object.put("Hsl", payrollExcel.getAdvancePayroll());
                        map.add(object);}
                    {JSONObject object = new JSONObject();
                        object.put("Rcntr", costCenter);
                        map.add(object);}
                    {JSONObject object = new JSONObject();
                        object.put("Kunnr", "");
                        map.add(object);}
                    {JSONObject object = new JSONObject();
                        object.put("Ksl", hulDunAdvancePayroll);
                        map.add(object);}

                    stringJson6 = map.toString();
                    stringJson6 = stringJson6.replace("{", "");
                    stringJson6 = stringJson6.replace("}", "");
                    stringJson6 = stringJson6.replace("]", "}");
                    stringJson6 = stringJson6.replace("[", "{");
                    itemList6.append(", ").append(stringJson6);
                }
            }
            String theString1 = itemList1.substring(2);
            String theString2 = itemList2.substring(2);
            String theString3 = itemList3.substring(2);
            String theString4 = itemList4.substring(2);
            String theString5 = itemList5.substring(2);
            String theString6 = itemList6.substring(2);
            theString1 = "{\n" +
                    "    \"IInfo\": {\n" +
                    "    \"Source\": \"SOAP\", \n" +
                    "    \"Destination\": \"SAP\", \n" +
                    "    \"Zdate\": \""+zDate+"\", \n" +
                    "    \"Ztime\": " +Integer.valueOf(zTime)+ " \n" +
                    "    },\n" +
                    "    \"ItInput\": {\n" +
                    "        \"item\": ["+theString1+"]\n" +
                    "    }\n" +
                    "}";
            theString2 = "{\n" +
                    "    \"IInfo\": {\n" +
                    "    \"Source\": \"SOAP\", \n" +
                    "    \"Destination\": \"SAP\", \n" +
                    "    \"Zdate\": \""+zDate+"\", \n" +
                    "    \"Ztime\": " +Integer.valueOf(zTime)+ " \n" +
                    "    },\n" +
                    "    \"ItInput\": {\n" +
                    "        \"item\": ["+theString2+"]\n" +
                    "    }\n" +
                    "}";
            theString3 = "{\n" +
                    "    \"IInfo\": {\n" +
                    "    \"Source\": \"SOAP\", \n" +
                    "    \"Destination\": \"SAP\", \n" +
                    "    \"Zdate\": \""+zDate+"\", \n" +
                    "    \"Ztime\": " +Integer.valueOf(zTime)+ " \n" +
                    "    },\n" +
                    "    \"ItInput\": {\n" +
                    "        \"item\": ["+theString3+"]\n" +
                    "    }\n" +
                    "}";
            theString4 = "{\n" +
                    "    \"IInfo\": {\n" +
                    "    \"Source\": \"SOAP\", \n" +
                    "    \"Destination\": \"SAP\", \n" +
                    "    \"Zdate\": \""+zDate+"\", \n" +
                    "    \"Ztime\": " +Integer.valueOf(zTime)+ " \n" +
                    "    },\n" +
                    "    \"ItInput\": {\n" +
                    "        \"item\": ["+theString4+"]\n" +
                    "    }\n" +
                    "}";
            theString5 = "{\n" +
                    "    \"IInfo\": {\n" +
                    "    \"Source\": \"SOAP\", \n" +
                    "    \"Destination\": \"SAP\", \n" +
                    "    \"Zdate\": \""+zDate+"\", \n" +
                    "    \"Ztime\": " +Integer.valueOf(zTime)+ " \n" +
                    "    },\n" +
                    "    \"ItInput\": {\n" +
                    "        \"item\": ["+theString5+"]\n" +
                    "    }\n" +
                    "}";
            theString6 = "{\n" +
                    "    \"IInfo\": {\n" +
                    "    \"Source\": \"SOAP\", \n" +
                    "    \"Destination\": \"SAP\", \n" +
                    "    \"Zdate\": \""+zDate+"\", \n" +
                    "    \"Ztime\": " +Integer.valueOf(zTime)+ " \n" +
                    "    },\n" +
                    "    \"ItInput\": {\n" +
                    "        \"item\": ["+theString6+"]\n" +
                    "    }\n" +
                    "}";
            System.out.println("Cost Center1: "+theString1);
            System.out.println("Cost Center2: "+theString2);
            System.out.println("Cost Center3: "+theString3);
            System.out.println("Cost Center4: "+theString4);
            System.out.println("Cost Center5: "+theString5);
            System.out.println("Cost Center6: "+theString6);
            postToSAP(theString1);
            postToSAP(theString2);
            postToSAP(theString3);
            postToSAP(theString4);
            postToSAP(theString5);
            postToSAP(theString6);
        }
        //EMPLOYEE: herev excel-d sheet ner n P7 baival ugugdul gej oilgon avah.
        if(worksheet7!=null){
            {
                String cellValue7 = getCellValue(worksheet7.getRow(5), 0);
                int index=4;
                while(cellValue7!=null && !cellValue7.equals("")){
                    if (!getCellValue(worksheet7.getRow(index), 0).equals("")) {
                        PayrollExcel payrollExcel = new PayrollExcel();
                        payrollExcel.setRegistryNumber(getCellValue(worksheet7.getRow(index), 0));
                        payrollExcel.setName(getCellValue(worksheet7.getRow(index), 1));
                        payrollExcel.setYear(Long.valueOf(getCellValue(worksheet7.getRow(index), 2)));
                        payrollExcel.setPeriod(Long.valueOf(getCellValue(worksheet7.getRow(index), 3)));
                        periodInExcel = payrollExcel.getPeriod();
                        yearInExcel = payrollExcel.getYear();
                        lastSerialNos = payrollExcelRepository.getSerialNosByYearAndPeriod(yearInExcel, periodInExcel);

                        long lastSerialNo;
                        if(lastSerialNos.size()>0){
                            lastSerialNo = lastSerialNos.get(0)+1;
                        }else {lastSerialNo = 1L;}

                        payrollExcel.setSerialNo(lastSerialNo);
                        payrollExcel.setPosition(getCellValue(worksheet7.getRow(index), 4));
                        payrollExcel.setDivision(getCellValue(worksheet7.getRow(index), 5));
                        payrollExcel.setCostCenter(getCellValue(worksheet7.getRow(index), 6));
                        payrollExcel.setEmployeeCode(getCellValue(worksheet7.getRow(index), 7));
                        if (isNumeric(getCellValue(worksheet7.getRow(index), 13))) {
                            payrollExcel.setArFromEmployee(new BigDecimal(getCellValue(worksheet7.getRow(index), 13)));
                        }

                        payrollExcel.setStatus(true);
                        payrollExcel.setIsLog(false);
                        payrollExcel.setCreatedDate(new Date());
                        result.add(payrollExcel);
                    }else {
                        cellValue7 = null;
                    }
                    index++;
                }
                payrollExcelRepository.saveAll(result);
                Thread.sleep(5 * 1000);

            List<PayrollExcel> payrollExcelList = payrollExcelRepository.getByCreatedDateBetweenAndSerialNoAndEmployeeCodeIsNotNull(startDate, endDate, lastSerial);
            int counter = 0, generalCounter=0, pushCount=0;

            BigDecimal ksl = new BigDecimal(0);
            for(PayrollExcel payrollExcel : payrollExcelList){
                generalCounter++;
                counter++;
                if(payrollExcel.getArFromEmployee()!=null){
                    ksl = ksl.add(payrollExcel.getArFromEmployee());
                }
                    List<JSONObject> map = new LinkedList<>();
                    {JSONObject object = new JSONObject();
                        object.put("Zhid", "P07"+subZHID+lastSerial);
                        map.add(object);}
                    sapIndex++;
                    {JSONObject object = new JSONObject();
                        object.put("Zinu", sapIndex);
                        map.add(object);}
                    {JSONObject object = new JSONObject();
                        object.put("Gjahr", payrollExcel.getYear());
                        map.add(object);}
                    {JSONObject object = new JSONObject();
                        object.put("Monat", payrollExcel.getPeriod());
                        map.add(object);}
                    {JSONObject object = new JSONObject();
                        object.put("Bukrs", 8000);
                        map.add(object);}
                    {JSONObject object = new JSONObject();
                        object.put("Waers", "MNT");
                        map.add(object);}
                    {JSONObject object = new JSONObject();
                        object.put("Zbty", "P07");
                        map.add(object);}
                    {JSONObject object = new JSONObject();
                        object.put("Hsl", payrollExcel.getArFromEmployee());
                        map.add(object);}
                    {JSONObject object = new JSONObject();
                        object.put("Rcntr", "");
                        map.add(object);}
                    {JSONObject object = new JSONObject();
                        object.put("Kunnr", payrollExcel.getEmployeeCode());
                        map.add(object);}
                    {JSONObject object = new JSONObject();
                        object.put("Ksl", 0);
                        map.add(object);}
                    String stringJson = map.toString();
                    stringJson = stringJson.replace("{", "");
                    stringJson = stringJson.replace("}", "");
                    stringJson = stringJson.replace("]", "}");
                    stringJson = stringJson.replace("[", "{");
                    itemList.append(", ").append(stringJson);

                    if(counter%100==0){
                        pushCount++;
                        String theString = itemList.substring(2);
                        theString = "{\n" +
                                "    \"IInfo\": {\n" +
                                "    \"Source\": \"SOAP\", \n" +
                                "    \"Destination\": \"SAP\", \n" +
                                "    \"Zdate\": \""+zDate+"\", \n" +
                                "    \"Ztime\": " +Integer.valueOf(zTime)+ " \n" +
                                "    },\n" +
                                "    \"ItInput\": {\n" +
                                "        \"item\": ["+theString+"]\n" +
                                "    }\n" +
                                "}";
//                        if(pushCount>=10){
//                            theString = theString.replace("\"Zhid\":\"P7\"", "\"Zhid\":\"P7"+subZHID+pushCount+"\"");
//                        }else{
//                            theString = theString.replace("\"Zhid\":\"P7\"", "\"Zhid\":\"P7"+subZHID+"0"+pushCount+"\"");
//                        }
                        theString = theString.replace("\"Ksl\":0", "\"Ksl\": "+ksl);
                        System.out.println("for each Employee: "+theString);
                        postToSAP(theString);
                        counter=0;
                        itemList = new StringBuilder();
                        ksl = new BigDecimal(0);
                    }
                    else if (payrollExcelList.size()==generalCounter){
                        pushCount++;
                        String theString = itemList.substring(2);
                        theString = "{\n" +
                                "    \"IInfo\": {\n" +
                                "    \"Source\": \"SOAP\", \n" +
                                "    \"Destination\": \"SAP\", \n" +
                                "    \"Zdate\": \""+zDate+"\", \n" +
                                "    \"Ztime\": " +Integer.valueOf(zTime)+ " \n" +
                                "    },\n" +
                                "    \"ItInput\": {\n" +
                                "        \"item\": ["+theString+"]\n" +
                                "    }\n" +
                                "}";
//                        if(pushCount>=10){
//                            theString = theString.replace("\"Zhid\":\"P7\"", "\"Zhid\":\"P7"+subZHID+pushCount+"\"");
//                        }else{
//                            theString = theString.replace("\"Zhid\":\"p7\"", "\"Zhid\":\"p7"+subZHID+"0"+pushCount+"\"");
//                        }

                        theString = theString.replace("\"Ksl\":0", "\"Ksl\": "+ksl);
                        System.out.println("for each Employee: "+theString);
                        postToSAP(theString);
                    }
            }
            }
        }
        return ResponseEntity.ok("Success");
    }

    @Async
    public ResponseEntity<String> postPayrollExcelWorkBook(String path) throws ParseException, IOException {

        List<PayrollExcel> result = new ArrayList<>();

        XSSFWorkbook workbook = (XSSFWorkbook) WorkbookFactory.create(new File(path));
        Long periodInExcel=null, yearInExcel = null;
        XSSFSheet worksheet1 = workbook.getSheet("P1");
        XSSFSheet worksheet7 = workbook.getSheet("P7");

        String pattern = "yyyy-MM-dd";
        DateFormat df = new SimpleDateFormat(pattern);
        String timePattern = "HH:mm:ss";
        DateFormat dft = new SimpleDateFormat(timePattern);
        Date today = new Date();
        String zDate = df.format(today);
        String zTime = dft.format(today);
        zTime = zTime.replace(":", "");
        String patternZHID = "yyMM";
        DateFormat zhid = new SimpleDateFormat(patternZHID);
        String subZHID = zhid.format(today);

        Date nowDate = Calendar.getInstance().getTime();
        String nowDateString = new SimpleDateFormat("yyyy-MM-dd").format(nowDate);
        Date startDate = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").parse(nowDateString + " 00:00:00");
        Date endDate = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").parse(nowDateString + " 23:59:59");
        int sapIndex=0;
        List<Long> lastSerialNos;
        long lastSerial;
//        List<Long> lastSerialNos = payrollExcelRepository.getSerialNosByYearAndPeriod(yearInExcel, periodInExcel);
//        long lastSerial;
//        if(lastSerialNos.size()>0){
//            lastSerial = lastSerialNos.get(0);
//            for(Long lastSerialOne : lastSerialNos){
//                if(lastSerialOne>lastSerial){
//                    lastSerial = lastSerialOne;
//                }
//            }
//        }else{
//            lastSerial = 1L;
//        }
//
//        System.out.println("lastSerial: "+lastSerial);

        StringBuilder itemList = new StringBuilder();
        StringBuilder itemList1 = new StringBuilder();
        StringBuilder itemList2 = new StringBuilder();
        StringBuilder itemList3 = new StringBuilder();
        StringBuilder itemList4 = new StringBuilder();
        StringBuilder itemList5 = new StringBuilder();
        StringBuilder itemList6 = new StringBuilder();

        //COSTCENTER: herev excel-d sheet ner n P1 baival ugugdul gej oilgon avah.
            //local-d ugugduluudiig hadgalj bn.
        if(worksheet1!=null){
            String cellValue1 = getCellValue(worksheet1.getRow(5), 2);
            List<String> costCenterList = new ArrayList<>();
            List<String> duplicationCostCenter = new ArrayList<>();
            int index=4;

            while(cellValue1!=null && !cellValue1.equals("")){
                if (!getCellValue(worksheet1.getRow(index), 2).equals("")) {
                    if(!duplicationCostCenter.contains(getCellValue(worksheet1.getRow(index), 6))) {
                        PayrollExcel payrollExcel = new PayrollExcel();
                        payrollExcel.setYear(Long.valueOf(getCellValue(worksheet1.getRow(index), 2)));
                        payrollExcel.setPeriod(Long.valueOf(getCellValue(worksheet1.getRow(index), 3)));
                        yearInExcel = payrollExcel.getYear();
                        periodInExcel = payrollExcel.getPeriod();
                        lastSerialNos = payrollExcelRepository.getSerialNosByYearAndPeriod(yearInExcel, periodInExcel);

                        long lastSerialNo;
                        if (lastSerialNos.size() > 0) {
                            lastSerialNo = lastSerialNos.get(0) + 1;
                        } else {
                            lastSerialNo = 1L;
                        }

                        payrollExcel.setSerialNo(lastSerialNo);
                        payrollExcel.setCostCenter(getCellValue(worksheet1.getRow(index), 6));
                        if (!costCenterList.contains(payrollExcel.getCostCenter())) {
                            costCenterList.add(payrollExcel.getCostCenter());
                        }
                        if (!getCellValue(worksheet1.getRow(index), 8).equals("") && isNumeric(getCellValue(worksheet1.getRow(index), 8))) {
                            payrollExcel.setEmployeeWages(new BigDecimal(getCellValue(worksheet1.getRow(index), 8)));
                        }
                        if (!getCellValue(worksheet1.getRow(index), 9).equals("") && isNumeric(getCellValue(worksheet1.getRow(index), 9))) {
                            payrollExcel.setSocialInsuranceFromEmployee(new BigDecimal(getCellValue(worksheet1.getRow(index), 9)));
                        }
                        if (!getCellValue(worksheet1.getRow(index), 10).equals("") && isNumeric(getCellValue(worksheet1.getRow(index), 10))) {
                            payrollExcel.setSocialInsuranceFromCompany(new BigDecimal(getCellValue(worksheet1.getRow(index), 10)));
                        }
                        if (!getCellValue(worksheet1.getRow(index), 11).equals("") && isNumeric(getCellValue(worksheet1.getRow(index), 11))) {
                            payrollExcel.setPersonalIncomeTax(new BigDecimal(getCellValue(worksheet1.getRow(index), 11)));
                        }
                        if (!getCellValue(worksheet1.getRow(index), 12).equals("") && isNumeric(getCellValue(worksheet1.getRow(index), 12))) {
                            payrollExcel.setAdvancePayroll(new BigDecimal(getCellValue(worksheet1.getRow(index), 12)));
                        }
                        if (!getCellValue(worksheet1.getRow(index), 14).equals("") && isNumeric(getCellValue(worksheet1.getRow(index), 14))) {
                            payrollExcel.setTradeUnionFee(new BigDecimal(getCellValue(worksheet1.getRow(index), 14)));
                        }

                        payrollExcel.setStatus(true);
                        payrollExcel.setIsLog(false);
                        payrollExcel.setCreatedDate(new Date());
                        result.add(payrollExcel);
                        duplicationCostCenter.add(getCellValue(worksheet1.getRow(index), 6));
                    }
                }else {
                    cellValue1 = null;
                }
                index++;
            }
            System.out.println("cost center result size: " + result.size());
//            payrollExcelRepository.saveAll(result);


            //local-s suuld tatagdsan ugugdluudiig tsugluulan avch bolovsruulj bn.
            lastSerialNos = payrollExcelRepository.getSerialNosByYearAndPeriod(yearInExcel, periodInExcel);
            if(lastSerialNos.size()>0){
                lastSerial = lastSerialNos.get(0);
                for(Long lastSerialOne : lastSerialNos){
                    if(lastSerialOne>lastSerial){
                        lastSerial = lastSerialOne;
                    }
                }
            }else{
                lastSerial = 1L;
            }
            if(lastSerial==1){
                lastSerial=2;
            }
            BigDecimal hulDunEmployeeWage = BigDecimal.valueOf(payrollExcelRepository.hulDunEmployeeWages(lastSerial-1));
            BigDecimal hulDunEmployeeInsurance = BigDecimal.valueOf(payrollExcelRepository.hulDunSocialInsuranceFromEmployee(lastSerial-1));
            BigDecimal hulDunCompanyInsurance = BigDecimal.valueOf(payrollExcelRepository.hulDunSocialInsuranceFromCompany(lastSerial-1));
            BigDecimal hulDunTax = BigDecimal.valueOf(payrollExcelRepository.hulDunIncomeTax(lastSerial-1));
            BigDecimal hulDunAdvancePayroll = BigDecimal.valueOf(payrollExcelRepository.hulDunAdvancePayroll(lastSerial-1));
            BigDecimal hulDunTradeUnionFee = BigDecimal.valueOf(payrollExcelRepository.hulDunTradeUnionFee(lastSerial-1));

            costCenterList = payrollExcelRepository.getCostCenters(lastSerial-1);
            String stringJson1, stringJson2, stringJson3, stringJson4, stringJson5, stringJson6;
            for(String costCenter : costCenterList){
//                    BigDecimal totalEmployeeWage = BigDecimal.valueOf(Long.parseLong(String.valueOf(payrollExcelRepository.totalEmployeeWages(costCenter, startDate, endDate, lastSerial))));
//                    BigDecimal totalEmployeeInsurance = BigDecimal.valueOf(Long.parseLong(String.valueOf(payrollExcelRepository.totalSocialInsuranceFromEmployee(costCenter, startDate, endDate, lastSerial))));
//                    BigDecimal totalCompanyInsurance = BigDecimal.valueOf(Long.parseLong(String.valueOf(payrollExcelRepository.totalSocialInsuranceFromCompany(costCenter, startDate, endDate, lastSerial))));
//                    BigDecimal totalTax = BigDecimal.valueOf(Long.parseLong(String.valueOf(payrollExcelRepository.totalPersonalIncomeTax(costCenter, startDate, endDate, lastSerial))));
//                    BigDecimal totalAdvancePayroll = BigDecimal.valueOf(Long.parseLong(String.valueOf(payrollExcelRepository.totalAdvancePayroll(costCenter, startDate, endDate, lastSerial))));
//                    BigDecimal totalTradeUnionFee = BigDecimal.valueOf(Long.parseLong(String.valueOf(payrollExcelRepository.totalTradeUnionFee(costCenter, startDate, endDate, lastSerial))));
                PayrollExcel payrollExcel = payrollExcelRepository.getByCostCenterAndSerialNo(costCenter, lastSerial-1).get(0);

                {
                    List<JSONObject> map = new LinkedList<>();
                    {JSONObject object = new JSONObject();
                        object.put("Zhid", "P01"+subZHID+lastSerial);
                        map.add(object);}
                    sapIndex++;
                    {JSONObject object = new JSONObject();
                        object.put("Zinu", sapIndex);
                        map.add(object);}
                    {JSONObject object = new JSONObject();
                        object.put("Gjahr", payrollExcel.getYear());
                        map.add(object);}
                    {JSONObject object = new JSONObject();
                        object.put("Monat", payrollExcel.getPeriod());
                        map.add(object);}
                    {JSONObject object = new JSONObject();
                        object.put("Bukrs", 8000);
                        map.add(object);}
                    {JSONObject object = new JSONObject();
                        object.put("Waers", "MNT");
                        map.add(object);}
                    {JSONObject object = new JSONObject();
                        object.put("Zbty", "P01");
                        map.add(object);}
                    {JSONObject object = new JSONObject();
                        object.put("Hsl", payrollExcel.getEmployeeWages());
                        map.add(object);}
                    {JSONObject object = new JSONObject();
                        object.put("Rcntr", costCenter);
                        map.add(object);}
                    {JSONObject object = new JSONObject();
                        object.put("Kunnr", "");
                        map.add(object);}
                    {JSONObject object = new JSONObject();
                        object.put("Ksl", hulDunEmployeeWage);
                        map.add(object);}

                    stringJson1 = map.toString();
                    stringJson1 = stringJson1.replace("{", "");
                    stringJson1 = stringJson1.replace("}", "");
                    stringJson1 = stringJson1.replace("]", "}");
                    stringJson1 = stringJson1.replace("[", "{");
                    itemList1.append(", ").append(stringJson1);
                }
                {
                    List<JSONObject> map = new LinkedList<>();
                    {JSONObject object = new JSONObject();

                        object.put("Zhid", "P02"+subZHID+lastSerial);
                        map.add(object);}
                    sapIndex++;
                    {JSONObject object = new JSONObject();
                        object.put("Zinu", sapIndex);
                        map.add(object);}
                    {JSONObject object = new JSONObject();
                        object.put("Gjahr", payrollExcel.getYear());
                        map.add(object);}
                    {JSONObject object = new JSONObject();
                        object.put("Monat", payrollExcel.getPeriod());
                        map.add(object);}
                    {JSONObject object = new JSONObject();
                        object.put("Bukrs", 8000);
                        map.add(object);}
                    {JSONObject object = new JSONObject();
                        object.put("Waers", "MNT");
                        map.add(object);}
                    {JSONObject object = new JSONObject();
                        object.put("Zbty", "P02");
                        map.add(object);}
                    {JSONObject object = new JSONObject();
                        object.put("Hsl", payrollExcel.getSocialInsuranceFromCompany());
                        map.add(object);}
                    {JSONObject object = new JSONObject();
                        object.put("Rcntr", costCenter);
                        map.add(object);}
                    {JSONObject object = new JSONObject();
                        object.put("Kunnr", "");
                        map.add(object);}
                    {JSONObject object = new JSONObject();
                        object.put("Ksl", hulDunCompanyInsurance);
                        map.add(object);}
                    stringJson2 = map.toString();
                    stringJson2 = stringJson2.replace("{", "");
                    stringJson2 = stringJson2.replace("}", "");
                    stringJson2 = stringJson2.replace("]", "}");
                    stringJson2 = stringJson2.replace("[", "{");
                    itemList2.append(", ").append(stringJson2);
                }
                {
                    List<JSONObject> map = new LinkedList<>();
                    {JSONObject object = new JSONObject();
                        object.put("Zhid", "P03"+subZHID+lastSerial);
                        map.add(object);}
                    sapIndex++;
                    {JSONObject object = new JSONObject();
                        object.put("Zinu", sapIndex);
                        map.add(object);}
                    {JSONObject object = new JSONObject();
                        object.put("Gjahr", payrollExcel.getYear());
                        map.add(object);}
                    {JSONObject object = new JSONObject();
                        object.put("Monat", payrollExcel.getPeriod());
                        map.add(object);}
                    {JSONObject object = new JSONObject();
                        object.put("Bukrs", 8000);
                        map.add(object);}
                    {JSONObject object = new JSONObject();
                        object.put("Waers", "MNT");
                        map.add(object);}
                    {JSONObject object = new JSONObject();
                        object.put("Zbty", "P03");
                        map.add(object);}
                    {JSONObject object = new JSONObject();
                        object.put("Hsl", payrollExcel.getSocialInsuranceFromEmployee());
                        map.add(object);}
                    {JSONObject object = new JSONObject();
                        object.put("Rcntr", costCenter);
                        map.add(object);}
                    {JSONObject object = new JSONObject();
                        object.put("Kunnr", "");
                        map.add(object);}
                    {JSONObject object = new JSONObject();
                        object.put("Ksl", hulDunEmployeeInsurance);
                        map.add(object);}

                    stringJson3 = map.toString();
                    stringJson3 = stringJson3.replace("{", "");
                    stringJson3 = stringJson3.replace("}", "");
                    stringJson3 = stringJson3.replace("]", "}");
                    stringJson3 = stringJson3.replace("[", "{");
                    itemList3.append(", ").append(stringJson3);
                }
                {
                    List<JSONObject> map = new LinkedList<>();
                    {JSONObject object = new JSONObject();
                        object.put("Zhid", "P04"+subZHID+lastSerial);
                        map.add(object);}
                    sapIndex++;
                    {JSONObject object = new JSONObject();
                        object.put("Zinu", sapIndex);
                        map.add(object);}
                    {JSONObject object = new JSONObject();
                        object.put("Gjahr", payrollExcel.getYear());
                        map.add(object);}
                    {JSONObject object = new JSONObject();
                        object.put("Monat", payrollExcel.getPeriod());
                        map.add(object);}
                    {JSONObject object = new JSONObject();
                        object.put("Bukrs", 8000);
                        map.add(object);}
                    {JSONObject object = new JSONObject();
                        object.put("Waers", "MNT");
                        map.add(object);}
                    {JSONObject object = new JSONObject();
                        object.put("Zbty", "P04");
                        map.add(object);}
                    {JSONObject object = new JSONObject();
                        object.put("Hsl", payrollExcel.getPersonalIncomeTax());
                        map.add(object);}
                    {JSONObject object = new JSONObject();
                        object.put("Rcntr", costCenter);
                        map.add(object);}
                    {JSONObject object = new JSONObject();
                        object.put("Kunnr", "");
                        map.add(object);}
                    {JSONObject object = new JSONObject();
                        object.put("Ksl", hulDunTax);
                        map.add(object);}

                    stringJson4 = map.toString();
                    stringJson4 = stringJson4.replace("{", "");
                    stringJson4 = stringJson4.replace("}", "");
                    stringJson4 = stringJson4.replace("]", "}");
                    stringJson4 = stringJson4.replace("[", "{");
                    itemList4.append(", ").append(stringJson4);
                }
                {
                    List<JSONObject> map = new LinkedList<>();
                    {JSONObject object = new JSONObject();
                        object.put("Zhid", "P05"+subZHID+lastSerial);
                        map.add(object);}
                    sapIndex++;
                    {JSONObject object = new JSONObject();
                        object.put("Zinu", sapIndex);
                        map.add(object);}
                    {JSONObject object = new JSONObject();
                        object.put("Gjahr", payrollExcel.getYear());
                        map.add(object);}
                    {JSONObject object = new JSONObject();
                        object.put("Monat", payrollExcel.getPeriod());
                        map.add(object);}
                    {JSONObject object = new JSONObject();
                        object.put("Bukrs", 8000);
                        map.add(object);}
                    {JSONObject object = new JSONObject();
                        object.put("Waers", "MNT");
                        map.add(object);}
                    {JSONObject object = new JSONObject();
                        object.put("Zbty", "P05");
                        map.add(object);}
                    {JSONObject object = new JSONObject();
                        object.put("Hsl", payrollExcel.getTradeUnionFee());
                        map.add(object);}
                    {JSONObject object = new JSONObject();
                        object.put("Rcntr", costCenter);
                        map.add(object);}
                    {JSONObject object = new JSONObject();
                        object.put("Kunnr", "");
                        map.add(object);}
                    {JSONObject object = new JSONObject();
                        object.put("Ksl", hulDunTradeUnionFee);
                        map.add(object);}

                    stringJson5 = map.toString();
                    stringJson5 = stringJson5.replace("{", "");
                    stringJson5 = stringJson5.replace("}", "");
                    stringJson5 = stringJson5.replace("]", "}");
                    stringJson5 = stringJson5.replace("[", "{");
                    itemList5.append(", ").append(stringJson5);
                }
                {
                    List<JSONObject> map = new LinkedList<>();
                    {JSONObject object = new JSONObject();
                        object.put("Zhid", "P06"+subZHID+lastSerial);
                        map.add(object);}
                    sapIndex++;
                    {JSONObject object = new JSONObject();
                        object.put("Zinu", sapIndex);
                        map.add(object);}
                    {JSONObject object = new JSONObject();
                        object.put("Gjahr", payrollExcel.getYear());
                        map.add(object);}
                    {JSONObject object = new JSONObject();
                        object.put("Monat", payrollExcel.getPeriod());
                        map.add(object);}
                    {JSONObject object = new JSONObject();
                        object.put("Bukrs", 8000);
                        map.add(object);}
                    {JSONObject object = new JSONObject();
                        object.put("Waers", "MNT");
                        map.add(object);}
                    {JSONObject object = new JSONObject();
                        object.put("Zbty", "P06");
                        map.add(object);}
                    {JSONObject object = new JSONObject();
                        object.put("Hsl", payrollExcel.getAdvancePayroll());
                        map.add(object);}
                    {JSONObject object = new JSONObject();
                        object.put("Rcntr", costCenter);
                        map.add(object);}
                    {JSONObject object = new JSONObject();
                        object.put("Kunnr", "");
                        map.add(object);}
                    {JSONObject object = new JSONObject();
                        object.put("Ksl", hulDunAdvancePayroll);
                        map.add(object);}

                    stringJson6 = map.toString();
                    stringJson6 = stringJson6.replace("{", "");
                    stringJson6 = stringJson6.replace("}", "");
                    stringJson6 = stringJson6.replace("]", "}");
                    stringJson6 = stringJson6.replace("[", "{");
                    itemList6.append(", ").append(stringJson6);
                }
            }
            String theString1 = itemList1.substring(2);
            String theString2 = itemList2.substring(2);
            String theString3 = itemList3.substring(2);
            String theString4 = itemList4.substring(2);
            String theString5 = itemList5.substring(2);
            String theString6 = itemList6.substring(2);
            theString1 = "{\n" +
                    "    \"IInfo\": {\n" +
                    "    \"Source\": \"SOAP\", \n" +
                    "    \"Destination\": \"SAP\", \n" +
                    "    \"Zdate\": \""+zDate+"\", \n" +
                    "    \"Ztime\": " +Integer.valueOf(zTime)+ " \n" +
                    "    },\n" +
                    "    \"ItInput\": {\n" +
                    "        \"item\": ["+theString1+"]\n" +
                    "    }\n" +
                    "}";
            theString2 = "{\n" +
                    "    \"IInfo\": {\n" +
                    "    \"Source\": \"SOAP\", \n" +
                    "    \"Destination\": \"SAP\", \n" +
                    "    \"Zdate\": \""+zDate+"\", \n" +
                    "    \"Ztime\": " +Integer.valueOf(zTime)+ " \n" +
                    "    },\n" +
                    "    \"ItInput\": {\n" +
                    "        \"item\": ["+theString2+"]\n" +
                    "    }\n" +
                    "}";
            theString3 = "{\n" +
                    "    \"IInfo\": {\n" +
                    "    \"Source\": \"SOAP\", \n" +
                    "    \"Destination\": \"SAP\", \n" +
                    "    \"Zdate\": \""+zDate+"\", \n" +
                    "    \"Ztime\": " +Integer.valueOf(zTime)+ " \n" +
                    "    },\n" +
                    "    \"ItInput\": {\n" +
                    "        \"item\": ["+theString3+"]\n" +
                    "    }\n" +
                    "}";
            theString4 = "{\n" +
                    "    \"IInfo\": {\n" +
                    "    \"Source\": \"SOAP\", \n" +
                    "    \"Destination\": \"SAP\", \n" +
                    "    \"Zdate\": \""+zDate+"\", \n" +
                    "    \"Ztime\": " +Integer.valueOf(zTime)+ " \n" +
                    "    },\n" +
                    "    \"ItInput\": {\n" +
                    "        \"item\": ["+theString4+"]\n" +
                    "    }\n" +
                    "}";
            theString5 = "{\n" +
                    "    \"IInfo\": {\n" +
                    "    \"Source\": \"SOAP\", \n" +
                    "    \"Destination\": \"SAP\", \n" +
                    "    \"Zdate\": \""+zDate+"\", \n" +
                    "    \"Ztime\": " +Integer.valueOf(zTime)+ " \n" +
                    "    },\n" +
                    "    \"ItInput\": {\n" +
                    "        \"item\": ["+theString5+"]\n" +
                    "    }\n" +
                    "}";
            theString6 = "{\n" +
                    "    \"IInfo\": {\n" +
                    "    \"Source\": \"SOAP\", \n" +
                    "    \"Destination\": \"SAP\", \n" +
                    "    \"Zdate\": \""+zDate+"\", \n" +
                    "    \"Ztime\": " +Integer.valueOf(zTime)+ " \n" +
                    "    },\n" +
                    "    \"ItInput\": {\n" +
                    "        \"item\": ["+theString6+"]\n" +
                    "    }\n" +
                    "}";
            System.out.println("Cost Center1: "+theString1);
            System.out.println("Cost Center2: "+theString2);
            System.out.println("Cost Center3: "+theString3);
            System.out.println("Cost Center4: "+theString4);
            System.out.println("Cost Center5: "+theString5);
            System.out.println("Cost Center6: "+theString6);
            postToSAP(theString1);
            postToSAP(theString2);
            postToSAP(theString3);
            postToSAP(theString4);
            postToSAP(theString5);
            postToSAP(theString6);
        }

        //EMPLOYEE: herev excel-d sheet ner n P7 baival ugugdul gej oilgon avah.
        if(worksheet7!=null){
            List<String> duplicateEmployeeRegister = new ArrayList<>();
            String cellValue7 = getCellValue(worksheet7.getRow(5), 0);
            int index=4;

            periodInExcel = Long.valueOf(getCellValue(worksheet7.getRow(index), 3));
            yearInExcel = Long.valueOf(getCellValue(worksheet7.getRow(index), 2));
            lastSerialNos = payrollExcelRepository.getSerialNosByYearAndPeriod(yearInExcel, periodInExcel);
            long lastSerialNo;
            if(lastSerialNos.size()>0){
                lastSerialNo = lastSerialNos.get(0)+1;
            }else {lastSerialNo = 1L;}
            System.out.println("periodInExcel, yearInExcel, lastSerialNos: "+periodInExcel+", "+yearInExcel+", "+lastSerialNo);

            while(cellValue7!=null && !cellValue7.equals("")){

                if (!getCellValue(worksheet7.getRow(index), 0).equals("")) {
                    if(!duplicateEmployeeRegister.contains(getCellValue(worksheet7.getRow(index), 0))){
                        PayrollExcel payrollExcel = new PayrollExcel();
                        payrollExcel.setRegistryNumber(getCellValue(worksheet7.getRow(index), 0));
                        payrollExcel.setName(getCellValue(worksheet7.getRow(index), 1));
                        payrollExcel.setYear(yearInExcel);
                        payrollExcel.setPeriod(periodInExcel);
                        payrollExcel.setSerialNo(lastSerialNo);
                        payrollExcel.setPosition(getCellValue(worksheet7.getRow(index), 4));
                        payrollExcel.setDivision(getCellValue(worksheet7.getRow(index), 5));
                        payrollExcel.setCostCenter(getCellValue(worksheet7.getRow(index), 6));
                        payrollExcel.setEmployeeCode(getCellValue(worksheet7.getRow(index), 7));
                        if (isNumeric(getCellValue(worksheet7.getRow(index), 13))) {
                            payrollExcel.setArFromEmployee(new BigDecimal(getCellValue(worksheet7.getRow(index), 13)));
                        }
                        payrollExcel.setStatus(true);
                        payrollExcel.setIsLog(false);
                        payrollExcel.setCreatedDate(new Date());
                        result.add(payrollExcel);
                        duplicateEmployeeRegister.add(getCellValue(worksheet7.getRow(index), 0));
                    }
                }else {
                    System.out.println("employee result size: "+result.size());
                    if(result.size()>0){
//                        payrollExcelRepository.saveAll(result);
                        result = new ArrayList<>();
                    }
                    cellValue7 = null;
                }
                index++;
            }

            System.out.println("lastSerial: "+lastSerialNo);
            List<PayrollExcel> payrollExcelList = payrollExcelRepository.getByCreatedDateBetweenAndSerialNoAndEmployeeCodeIsNotNull(startDate, endDate, lastSerialNo);
            System.out.println("payrollExcelList size: "+payrollExcelList.size());
            int counter = 0, generalCounter=0, pushCount=0;

            BigDecimal ksl = new BigDecimal(0);
            for(PayrollExcel payrollExcel : payrollExcelList){
                generalCounter++;
                counter++;
                if(payrollExcel.getArFromEmployee()!=null){
                    ksl = ksl.add(payrollExcel.getArFromEmployee());
                }
                List<JSONObject> map = new LinkedList<>();
                {JSONObject object = new JSONObject();
                    object.put("Zhid", "P07"+subZHID+lastSerialNo);
                    map.add(object);}
                sapIndex++;
                {JSONObject object = new JSONObject();
                    object.put("Zinu", sapIndex);
                    map.add(object);}
                {JSONObject object = new JSONObject();
                    object.put("Gjahr", payrollExcel.getYear());
                    map.add(object);}
                {JSONObject object = new JSONObject();
                    object.put("Monat", payrollExcel.getPeriod());
                    map.add(object);}
                {JSONObject object = new JSONObject();
                    object.put("Bukrs", 8000);
                    map.add(object);}
                {JSONObject object = new JSONObject();
                    object.put("Waers", "MNT");
                    map.add(object);}
                {JSONObject object = new JSONObject();
                    object.put("Zbty", "P07");
                    map.add(object);}
                {JSONObject object = new JSONObject();
                    object.put("Hsl", payrollExcel.getArFromEmployee());
                    map.add(object);}
                {JSONObject object = new JSONObject();
                    object.put("Rcntr", "");
                    map.add(object);}
                {JSONObject object = new JSONObject();
                    object.put("Kunnr", payrollExcel.getEmployeeCode());
                    map.add(object);}
                {JSONObject object = new JSONObject();
                    object.put("Ksl", 0);
                    map.add(object);}
                String stringJson = map.toString();
                stringJson = stringJson.replace("{", "");
                stringJson = stringJson.replace("}", "");
                stringJson = stringJson.replace("]", "}");
                stringJson = stringJson.replace("[", "{");
                itemList.append(", ").append(stringJson);

                if(counter%100==0){
                    pushCount++;
                    String theString = itemList.substring(2);
                    theString = "{\n" +
                            "    \"IInfo\": {\n" +
                            "    \"Source\": \"SOAP\", \n" +
                            "    \"Destination\": \"SAP\", \n" +
                            "    \"Zdate\": \""+zDate+"\", \n" +
                            "    \"Ztime\": " +Integer.valueOf(zTime)+ " \n" +
                            "    },\n" +
                            "    \"ItInput\": {\n" +
                            "        \"item\": ["+theString+"]\n" +
                            "    }\n" +
                            "}";
                    theString = theString.replace("\"Ksl\":0", "\"Ksl\": "+ksl);
                    System.out.println("for each Employee: "+theString);
                    postToSAP(theString);
                    counter=0;
                    itemList = new StringBuilder();
                    ksl = new BigDecimal(0);
                }
                else if (payrollExcelList.size()==generalCounter){
                    pushCount++;
                    String theString = itemList.substring(2);
                    theString = "{\n" +
                            "    \"IInfo\": {\n" +
                            "    \"Source\": \"SOAP\", \n" +
                            "    \"Destination\": \"SAP\", \n" +
                            "    \"Zdate\": \""+zDate+"\", \n" +
                            "    \"Ztime\": " +Integer.valueOf(zTime)+ " \n" +
                            "    },\n" +
                            "    \"ItInput\": {\n" +
                            "        \"item\": ["+theString+"]\n" +
                            "    }\n" +
                            "}";
                    theString = theString.replace("\"Ksl\":0", "\"Ksl\": "+ksl);
                    System.out.println("for each Employee: "+theString);
                    postToSAP(theString);
                }
            }
        }

        return ResponseEntity.ok("Success");
    }

    public static boolean isNumeric(String strNum) {
        if (strNum == null) {
            return false;
        }
        try {
            double d = Double.parseDouble(strNum);
        } catch (NumberFormatException nfe) {
            return false;
        }
        return true;
    }

    public static void postToSAP(String requestString) {

        HttpEntity<String> request = new HttpEntity<>(requestString, Util.internalPostHeaders());
        ResponseEntity<String> responseEntity;

        final RestTemplate restTemplate = new RestTemplate();

        try{
            responseEntity = restTemplate.exchange(
                    Util.payrollPostUrlToSAP,
                    HttpMethod.POST, request, String.class);

            String result = responseEntity.getBody();

            System.out.println(result);

            JSONParser parser = new JSONParser();
            parser.parse(result);
        }catch (Exception e){
            System.out.println("rest template exception: "+e);
        }
    }
}
