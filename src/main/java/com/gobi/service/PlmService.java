package com.gobi.service;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.S3Object;
import com.gobi.model.Plm;
import com.gobi.repository.PlmRepository;
import com.gobi.util.Util;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.minidev.json.JSONObject;
import net.minidev.json.parser.JSONParser;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.client.RestTemplate;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.util.*;

@Service
@Slf4j
@RequiredArgsConstructor
public class PlmService {

    @Autowired
    PlmRepository plmRepository;
    @Value("${filePath}")
    String filePath;
    @Value("${aws.s3.bucket}")
    private String bucketName;

    private final AmazonS3 s3Client;

    public Page<Plm> findAll(Specification<Plm> specs, Pageable pageable) {
        return plmRepository.findAll(Specification.where(specs), pageable);
    }

    public boolean postPlm(@RequestParam("fileName")  @NotBlank @NotNull String fileName) {
        S3Object object = s3Client.getObject(bucketName, fileName);
        if(object !=null) {
                    List<Plm> plms = csvConvertToList(object.getObjectContent());
                    plmRepository.saveAll(plms);
            return true;
        }else{
            return false;
        }
    }

    public List<Plm> csvConvertToList(InputStream is)  {
        try (BufferedReader fileReader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8));
             CSVParser csvParser = new CSVParser(fileReader, CSVFormat.DEFAULT.withFirstRecordAsHeader()
                     .withIgnoreHeaderCase().withTrim())) {

            List<Plm> plms = new ArrayList<>();
            List<CSVRecord> csvRecords = csvParser.getRecords();

            String pattern = "yyyy-MM-dd";
            DateFormat df = new SimpleDateFormat(pattern);
            String timePattern = "HH:mm:ss";
            DateFormat dft = new SimpleDateFormat(timePattern);

            Date today = new Date();
            String zDate = df.format(today);
            String zTime = dft.format(today);
            zTime = zTime.replace(":", "");
            if(zTime.startsWith("0")){
                zTime = zTime.substring(1);
            }

            StringBuilder itemList = new StringBuilder();
            List<Plm> savedPlmListSortedById = plmRepository.findAllByCreatedDateIsNotNullOrderByIdDesc();
            int lastId;
            if(savedPlmListSortedById.size()>0){
                lastId = Math.toIntExact(savedPlmListSortedById.get(0).getId());
            }else{
                lastId = 0;
            }
            int i=0, counter=0, generalCounter=0;
            for (CSVRecord csvRecord : csvRecords) {
                Plm plm = new Plm();
                i++;counter++;generalCounter++;
//                plm.setPlmId(csvRecord.get(0));
                plm.setMaterialCode(csvRecord.get(0));
                plm.setMaterialDescription(csvRecord.get(1));
                plm.setUnitOfMeasureBaseUnit(csvRecord.get(2));
                plm.setMaterialType(csvRecord.get(3));
                if(!csvRecord.get(4).equals("") && isNumeric(csvRecord.get(4))){
                    String labString = csvRecord.get(4);
                    if(labString.startsWith("0")){
                        labString = labString.substring(1);
                    }
                    System.out.println("labString: "+labString);
                    plm.setLaboratoryDesignOfficeCategory(labString);
                }
                plm.setMaterialGroupCategorySubCategory(csvRecord.get(5));
                plm.setDivision(csvRecord.get(6));
                plm.setIndustryStandartDescriptionDepartment(csvRecord.get(7));
                if(!csvRecord.get(8).equals("") && isNumeric(csvRecord.get(8)) ){
                    plm.setGrossWeight(csvRecord.get(8));
                }
                if(!csvRecord.get(9).equals("")){
                    plm.setWeightUnit(csvRecord.get(9));
                }
                if(!csvRecord.get(10).equals("") && isNumeric(csvRecord.get(10))){
                    plm.setNetWeight(csvRecord.get(10));
                }
                plm.setExternalMaterialGroupBrand(csvRecord.get(11));
                plm.setBasicMaterialStyle(csvRecord.get(12));
                plm.setMaterialLongDescription(csvRecord.get(13));
                plm.setSize(csvRecord.get(14));
                plm.setColor(csvRecord.get(15));
                plm.setCollection(csvRecord.get(16));
                plm.setSeason(csvRecord.get(17));
                plm.setGauge(csvRecord.get(18));
                plm.setPattern(csvRecord.get(19));
                plm.setMainMaterialComposition(csvRecord.get(20));
                plm.setYarnCount(csvRecord.get(21));
                plm.setOnlineCode(csvRecord.get(22));
                plm.setColorName(csvRecord.get(23));
                plm.setKidsClassification(csvRecord.get(24));
                plm.setMaterialName(csvRecord.get(25));
                plm.setAdditionalInformation(csvRecord.get(26));

                LocalDate currentDate = LocalDate.now();
                plm.setPlmYear((long) currentDate.getYear());
                plm.setPlmMonth((long) currentDate.getMonthValue());
                plm.setPlmDay((long) currentDate.getDayOfMonth());
                plm.setCreatedDate(new Date());
                plm.setStatus(true);
                plm.setIsLog(false);
                plms.add(plm);

                List<JSONObject> map = new LinkedList<>();
                {
                JSONObject object = new JSONObject();
                int theId = lastId+i;

                object.put("Zplmid", Integer.toString(theId));
                map.add(object);
                }
                //Mandatory
                if (plm.getMaterialCode() != null && plm.getMaterialDescription() != null && plm.getUnitOfMeasureBaseUnit() != null && plm.getMaterialType() != null &&
                        plm.getLaboratoryDesignOfficeCategory() != null && plm.getMaterialGroupCategorySubCategory() != null && plm.getDivision() != null) {
                            //Matnr Mandatory
                            {
                                JSONObject object = new JSONObject();
                                String matnr = plm.getMaterialCode();
                                if(matnr.length()>40){
                                    matnr = plm.getMaterialCode().substring(0, 40);
                                }
                                object.put("Matnr", matnr);
                                map.add(object);
                            }
                            //Maktx Mandatory - item description
                            {
                                JSONObject object = new JSONObject();
                                String maktx;
//                                String code = plm.getMaterialCode().substring(0, 2);
                                String code = plm.getMaterialGroupCategorySubCategory();
                                switch (code){
                                    case "AB":
                                    case "AD":
                                    case "AF":
                                    case "AK":
                                    case "AW":
                                    case "OJ":
                                        maktx = plm.getMaterialName()+", "+plm.getMaterialDescription()+", "+plm.getAdditionalInformation()+", "+plm.getMainMaterialComposition(); break;
                                    case "AH":
                                    case "AI":
                                    case "AZ":
                                        maktx = plm.getMaterialName()+", "+plm.getAdditionalInformation()+", "+plm.getMainMaterialComposition(); break;
                                    case "AR":
                                    case "OO":
                                        maktx = plm.getMaterialName()+", "+plm.getMaterialDescription()+", "+plm.getAdditionalInformation(); break;
                                    case "AS": maktx = plm.getMaterialName()+", "+plm.getAdditionalInformation()+", "+plm.getMaterialDescription(); break;
                                    case "LC": maktx = plm.getMaterialName()+", "+plm.getSize()+", "+plm.getMainMaterialComposition(); break;
                                    case "LE":
                                    case "LL":
                                    case "OT":
                                    case "OU":
                                        maktx = plm.getMaterialDescription()+", "+plm.getAdditionalInformation()+", "+plm.getMainMaterialComposition(); break;
                                    case "PG":
                                    case "PJ":
                                    case "PM":
                                    case "PN":
                                    case "PO":
                                    case "PP":
                                    case "PV":
                                    case "PX":
                                    case "PY":
                                    case "PC":
                                    case "PF":
                                    case "PH":
                                    case "PI":
                                    case "PS":
                                    case "PZ":
                                        maktx = plm.getMaterialDescription()+", "+plm.getAdditionalInformation();break;
                                    case "" +
                                            "":
                                    case "CD":
                                        maktx = plm.getMaterialName()+", "+plm.getMaterialDescription();break;
                                    case "YK":
                                    case "YW":
                                        maktx = plm.getMaterialName()+", "+plm.getColor()+", "+plm.getMainMaterialComposition();break;
                                    case "46":
                                    case "47":
                                    case "48":
                                    case "65":
                                    case "89":
                                    case "90":
                                        maktx = plm.getMaterialName()+", "+plm.getColor()+", "+plm.getMaterialDescription();break;
                                    case "OV":
                                        maktx = plm.getMaterialName()+", "+plm.getMaterialDescription()+", "+plm.getColor() +", "+plm.getAdditionalInformation();break;
                                    default: maktx = plm.getMaterialDescription();break;
                                }
                                if(maktx.length()>40){
                                    maktx = maktx.substring(0, 40);
                                }
                                plm.setSapMaterialDescription(maktx);
                                object.put("Maktx", maktx);
                                map.add(object);
                            }
                            //Meins Mandatory Switch
                            {
                                JSONObject object = new JSONObject();
                                object.put("Meins", plm.getUnitOfMeasureBaseUnit());
                                switch (plm.getUnitOfMeasureBaseUnit().toUpperCase()) {
                                    case "GRAM":
                                        object.put("Meins", Util.Gram_MEINS);
                                        break;
                                    case "KILOGRAM":
                                        object.put("Meins", Util.Kilogram_MEINS);
                                        break;
                                    case "LITER":
                                        object.put("Meins", Util.Liter_MEINS);
                                        break;
                                    case "METER":
                                        object.put("Meins", Util.Meter_MEINS);
                                        break;
                                    case "SQUARE METER":
                                        object.put("Meins", Util.Square_Meter_MEINS);
                                        break;
                                    case "CUBIC METER":
                                        object.put("Meins", Util.Cubic_Meter_MEINS);
                                        break;
                                    case "MILLIGRAM":
                                        object.put("Meins", Util.Milligram_MEINS);
                                        break;
                                    case "BOTTLE":
                                        object.put("Meins", Util.Bottle_MEINS);
                                        break;
                                    case "CENTIMETER":
                                        object.put("Meins", Util.Centimeter_MEINS);
                                        break;
                                    case "SQUARE CENTIMETER":
                                        object.put("Meins", Util.Square_Centimeter_MEINS);
                                        break;
                                    case "DAYS":
                                        object.put("Meins", Util.Days_MEINS);
                                        break;
                                    case "DEGREE":
                                        object.put("Meins", Util.Degree_MEINS);
                                        break;
                                    case "DECIMETER":
                                        object.put("Meins", Util.Decimeter_MEINS);
                                        break;
                                    case "DOZEN":
                                        object.put("Meins", Util.Dozen_MEINS);
                                        break;
                                    case "GRAM ACTIVE INGREDIENT/LITER":
                                        object.put("Meins", Util.GramActive_IngredientLiter_MEINS);
                                        break;
                                    case "GRAM/LITER":
                                        object.put("Meins", Util.GramLiter_MEINS);
                                        break;
                                    case "GRAM/SQUARE METER":
                                        object.put("Meins", Util.GramSquare_Meter_MEINS);
                                        break;
                                    case "GRAM/CUBIC METER":
                                        object.put("Meins", Util.GramCubic_Meter_MEINS);
                                        break;
                                    case "GROSS":
                                        object.put("Meins", Util.Gross_MEINS);
                                        break;
                                    case "HOUR":
                                        object.put("Meins", Util.Hour_MEINS);
                                        break;
                                    case "HOURS":
                                        object.put("Meins", Util.Hours_MEINS);
                                        break;
                                    case "KILOGRAM/CUBIC DECIMETER":
                                        object.put("Meins", Util.KilogramGubic_Decimeter_MEINS);
                                        break;
                                    case "KILOGRAM/MOLE":
                                        object.put("Meins", Util.KilogramMole_MEINS);
                                        break;
                                    case "KILOGRAM/SECOND":
                                        object.put("Meins", Util.KilogramSecond_MEINS);
                                        break;
                                    case "KILOGRAM/CUBIC METER":
                                        object.put("Meins", Util.KilogramCubic_Meter_MEINS);
                                        break;
                                    case "1/SQUARE METER":
                                        object.put("Meins", Util.OneSquare_Meter_MEINS);
                                        break;
                                    case "MILLIGRAM/LITER":
                                        object.put("Meins", Util.MilligramLiter_MEINS);
                                        break;
                                    case "MINUTE":
                                        object.put("Meins", Util.Minute_MEINS);
                                        break;
                                    case "MILLILITER":
                                        object.put("Meins", Util.Milliliter_MEINS);
                                        break;
                                    case "MILLIMETER":
                                        object.put("Meins", Util.Millimeter_MEINS);
                                        break;
                                    case "MONTHS":
                                        object.put("Meins", Util.Months_MEINS);
                                        break;
                                    case "NANOMETER":
                                        object.put("Meins", Util.Nanometer_MEINS);
                                        break;
                                    case "KILONEWTON":
                                        object.put("Meins", Util.Kilonewton_MEINS);
                                        break;
                                    case "PACK":
                                        object.put("Meins", Util.Pack_MEINS);
                                        break;
                                    case "PIECE":
                                        object.put("Meins", Util.Piece_MEINS);
                                        break;
                                    case "NUMBER OF PERSONS":
                                        object.put("Meins", Util.NumberOfPersons_MEINS);
                                        break;
                                    case "TON":
                                        object.put("Meins", Util.Ton_MEINS);
                                        break;
                                    case "THOUSANDS":
                                        object.put("Meins", Util.Thousands_MEINS);
                                        break;
                                    case "WEEKS":
                                        object.put("Meins", Util.Weeks_MEINS);
                                        break;
                                    case "YEARS":
                                        object.put("Meins", Util.Years_MEINS);
                                        break;
                                    case "PIECES":
                                        object.put("Meins", Util.Pieces_MEINS);
                                        break;
                                    default:
                                        if(plm.getUnitOfMeasureBaseUnit().length()>3){
                                            object.put("Meins", plm.getUnitOfMeasureBaseUnit().toUpperCase().substring(0, 3));
                                        }else{
                                            object.put("Meins", plm.getUnitOfMeasureBaseUnit().toUpperCase());
                                        }
                                }
                                map.add(object);
                            }
                            //Mtart Mandatory Switch
                            {
                                JSONObject object = new JSONObject();
                                switch (plm.getMaterialType().toUpperCase()) {
                                    case "AUXILIARY":
                                        object.put("Mtart", Util.Auxiliary_Materials_MTART);
                                        break;
                                    case "LABELS":
                                        object.put("Mtart", Util.Label_MTART);
                                        break;
                                    case "CHEMICAL":
                                        object.put("Mtart", Util.Chemistry_MTART);
                                        break;
                                    case "PACKAGING":
                                        object.put("Mtart", Util.Package_MTART);
                                        break;
                                    case "FABRIC":
                                        object.put("Mtart", Util.Fabrics_MTART);
                                        break;
                                    case "PRINT":
                                        object.put("Mtart", Util.Prints_MTART);
                                        break;
                                    case "EMBROIDERY":
                                        object.put("Mtart", Util.Embroidery_MTART);
                                        break;
                                    case "OTHER":
                                        object.put("Mtart", Util.Others_MTART);
                                        break;
                                    case "YARN":
                                    case "CO-PRODUCT":
                                        object.put("Mtart", Util.Yarn_Coproduct_MTART);
                                        break;
                                    case "KNIT":
                                        object.put("Mtart", Util.Knit_MTART);
                                        break;
                                    case "WOVEN":
                                        object.put("Mtart", Util.Woven_MTART);
                                        break;
                                    case "SEWN":
                                        object.put("Mtart", Util.Sewn_MTART);
                                        break;
                                    case "CUSTOM":
                                        object.put("Mtart", Util.Custom_Made_MTART);
                                        break;
                                    case "RAW MATERIALS":
                                        object.put("Mtart", Util.Raw_Material_Cashmere_MTART);
                                        break;
                                    case "ELECTRONIC":
                                    case "STANDART PARTS":
                                    case "IMPORT PRODUCT":
                                    case "OTHER PRODUCT":
                                    case "DOCUMENT":
                                    case "OFFICE":
                                        object.put("Mtart", Util.Raw_Material_Electronic_Etc_MTART);
                                        break;
                                    case "SEMI PRODUCT":
                                        object.put("Mtart", Util.Semi_Product_MTART);
                                        break;
                                    case "SERVICE":
                                        object.put("Mtart", Util.Services_MTART);
                                        break;
                                    default:
                                        if(plm.getMaterialType().length()>4){
                                            object.put("Mtart", plm.getMaterialType().toUpperCase().substring(0, 4));
                                        }else{
                                            object.put("Mtart", plm.getMaterialType().toUpperCase());
                                        }
                                }
                                map.add(object);
                            }
                            {
                                JSONObject object = new JSONObject();
                                object.put("Mbrsh", "M");
                                map.add(object);
                            }
                            {
                                JSONObject object = new JSONObject();
                                if(plm.getLaboratoryDesignOfficeCategory().length()>3){
                                    object.put("Labor", plm.getLaboratoryDesignOfficeCategory().substring(0, 3));
                                }else{
                                    object.put("Labor", plm.getLaboratoryDesignOfficeCategory());
                                }
                                map.add(object);
                            }
                            //Matkl Mandatory Switch
                            {
                                JSONObject object = new JSONObject();
                                object.put("Matkl", plm.getMaterialGroupCategorySubCategory());
                                switch (plm.getMaterialGroupCategorySubCategory().toUpperCase()) {
                                    case "POLO":
                                        object.put("Matkl", Util.Polo_MATKL);
                                        break;
                                    case "CARDIGAN":
                                        object.put("Matkl", Util.Cardigan_MATKL);
                                        break;
                                    case "TOP":
                                        object.put("Matkl", Util.Top_MATKL);
                                        break;
                                    case "C-NECK":
                                        object.put("Matkl", Util.C_Neck_MATKL);
                                        break;
                                    case "T-NECK":
                                        object.put("Matkl", Util.T_Neck_MATKL);
                                        break;
                                    case "HIGH NECK":
                                        object.put("Matkl", Util.High_Neck_MATKL);
                                        break;
                                    case "V-NECK":
                                        object.put("Matkl", Util.V_Neck_MATKL);
                                        break;
                                    case "DRESS":
                                        object.put("Matkl", Util.Dress_MATKL);
                                        break;
                                    case "VEST":
                                        object.put("Matkl", Util.Vest_MATKL);
                                        break;
                                    case "HAT":
                                        object.put("Matkl", Util.Hat_MATKL);
                                        break;
                                    case "SOCKS":
                                        object.put("Matkl", Util.Socks_MATKL);
                                        break;
                                    case "HOODIE":
                                        object.put("Matkl", Util.Hoodie_MATKL);
                                        break;
                                    case "COAT":
                                        object.put("Matkl", Util.Coat_MATKL);
                                        break;
                                    case "JACKET":
                                        object.put("Matkl", Util.Jacket_MATKL);
                                        break;
                                    case "PANTS":
                                        object.put("Matkl", Util.Pants_MATKL);
                                        break;
                                    case "SKIRT":
                                        object.put("Matkl", Util.Skirt_MATKL);
                                        break;
                                    case "PONCHO":
                                        object.put("Matkl", Util.Poncho_MATKL);
                                        break;
                                    case "SHORT":
                                        object.put("Matkl", Util.Short_MATKL);
                                        break;
                                    case "GLOVES":
                                        object.put("Matkl", Util.Gloves_MATKL);
                                        break;
                                    case "SHAWL":
                                        object.put("Matkl", Util.Shawl_MATKL);
                                        break;
                                    case "SCARF":
                                        object.put("Matkl", Util.Scarf_MATKL_01);
                                        break;
                                    case "WARMER BAG":
                                        object.put("Matkl", Util.Warmer_Bag_MATKL);
                                        break;
                                    case "BLANKET":
                                        object.put("Matkl", Util.Blanket_MATKL);
                                        break;
                                    case "HEAD BAND":
                                        object.put("Matkl", Util.Head_Band_MATKL);
                                        break;
                                    case "PILLOWCASE":
                                        object.put("Matkl", Util.Pillowcase_MATKL);
                                        break;
                                    case "BAG":
                                        object.put("Matkl", Util.Bag_MATKL);
                                        break;
                                    case "BATHROBE":
                                        object.put("Matkl", Util.Hoodie_MATKL);
                                        break;
                                    case "BRALETTE":
                                    case "BACK WARMER":
                                        object.put("Matkl", Util.Bralette_BackWarmer_MATKL);
                                        break;
                                    case "DETAIL":
                                    case "SWATCH":
                                        object.put("Matkl", Util.Detail_Swatch_MATKL_01);
                                        break;
                                    case "BELT":
                                        object.put("Matkl", Util.Belt_MATKL);
                                        break;
                                    case "LEG WARMER":
                                        object.put("Matkl", Util.Leg_Warmer_MATKL);
                                        break;
                                    case "BABY ROMPERS":
                                        object.put("Matkl", Util.Baby_Rompers_MATKL);
                                        break;
                                    case "BABY SHOES":
                                        object.put("Matkl", Util.Baby_Shoes_MATKL);
                                        break;
                                    case "EYE MASK":
                                        object.put("Matkl", Util.Eye_Mask_MATKL);
                                        break;
                                    case "SANDALS":
                                        object.put("Matkl", Util.Sandals_MATKL);
                                        break;
                                    case "BABY BLANKET":
                                        object.put("Matkl", Util.Baby_Blanket_MATKL);
                                        break;
                                    case "NECKTIE":
                                        object.put("Matkl", Util.Necktie_MATKL);
                                        break;
                                    case "SUITS":
                                        object.put("Matkl", Util.Suits_MATKL);
                                        break;
                                    case "FABRIC":
                                        object.put("Matkl", Util.Fabric_MATKL);
                                        break;
                                    case "SEMI WORSTED FABRIC":
                                        object.put("Matkl", Util.Semi_Worsted_Fabric_MATKL);
                                        break;
                                    case "KNITTED FABRIC":
                                        object.put("Matkl", Util.Knitted_Fabric_MATKL);
                                        break;
                                    case "OTHER":
                                        object.put("Matkl", Util.Other_MATKL);
                                        break;
                                    case "CHAIR CUSHION":
                                        object.put("Matkl", Util.Chair_Cushion_MATKL);
                                        break;
                                    case "KNEE WARMER":
                                        object.put("Matkl", Util.Knee_Warmer_MATKL);
                                        break;
                                    case "BED RUNNER":
                                        object.put("Matkl", Util.Bed_Runner_MATKL);
                                        break;
                                    case "ROBE OF LAMA":
                                        object.put("Matkl", Util.Robe_Of_Lama_MATKL);
                                        break;
                                    case "COMFORTER":
                                        object.put("Matkl", Util.Comforter_MATKL);
                                        break;
                                    case "SLEEPING BAG":
                                        object.put("Matkl", Util.Sleeping_Bag_MATKL);
                                        break;
                                    case "TOPPER":
                                        object.put("Matkl", Util.Topper_MATKL);
                                        break;
                                    case "WORSTED FABRIC":
                                        object.put("Matkl", Util.Worsted_Fabric_MATKL);
                                        break;
                                    case "SHOES":
                                        object.put("Matkl", Util.Shoes_MATKL);
                                        break;
                                    case "BLAZER":
                                        object.put("Matkl", Util.Blazer_MATKL);
                                        break;
                                    case "TRENCH COAT":
                                        object.put("Matkl", Util.Trench_Coat_MATKL);
                                        break;
                                    case "SINGLE SIDED FABRIC":
                                        object.put("Matkl", Util.Single_Sided_Fabric_MATKL);
                                        break;
                                    case "DOUBLE SIDED FABRIC":
                                        object.put("Matkl", Util.Double_Sided_Fabric_MATKL);
                                        break;
                                    case "TRADITIONAL CLOTH":
                                        object.put("Matkl", Util.Traditional_Cloth_MATKL);
                                        break;
                                    case "TRADITIONAL OVERDRESS":
                                        object.put("Matkl", Util.Traditional_Overdress_MATKL);
                                        break;
                                    case "TRADITIONAL VEST":
                                        object.put("Matkl", Util.Traditional_Vest_MATKL);
                                        break;
                                    case "TRADITIONAL TOPCOAT":
                                        object.put("Matkl", Util.Traditional_Topcoat_MATKL);
                                        break;
                                    case "TRADITIONAL SHIRT":
                                        object.put("Matkl", Util.Traditional_Shirt_MATKL);
                                        break;
                                    case "TRADITIONAL CLOTHING":
                                        object.put("Matkl", Util.Traditional_Clothing_MATKL);
                                        break;
                                    case "PRINTED SCARF":
                                        object.put("Matkl", Util.Printed_Scarf_MATKL);
                                        break;
                                    case "PRINTED SHAWL":
                                        object.put("Matkl", Util.Printed_Shawl_MATKL);
                                        break;
                                    case "PLUMBING SPARE PARTS":
                                        object.put("Matkl", Util.Plumbing_Spare_Parts_MATKL);
                                        break;
                                    case "ELECTRICAL SPARE PARTS":
                                        object.put("Matkl", Util.Electrical_Spare_Parts_MATKL);
                                        break;
                                    case "ELECTRONIC AND TELEPHONE SPARE PARTS":
                                        object.put("Matkl", Util.Electronic_And_Telephone_Spare_Parts_MATKL);
                                        break;
                                    case "VENTILATION SPARE PARTS":
                                        object.put("Matkl", Util.Ventilation_Spare_Parts_MATKL);
                                        break;
                                    case "PRIMARY-WASHING SPARE PARTS":
                                        object.put("Matkl", Util.PrimaryWashing_Spare_Parts_MATKL);
                                        break;
                                    case "PRIMARY-DEHAIRING SPARE PARTS":
                                        object.put("Matkl", Util.PrimaryDehairing_Spare_Parts_MATKL);
                                        break;
                                    case "PRIMARY-PRESSING AND SORTING SPARE PARTS":
                                        object.put("Matkl", Util.PrimaryPressing_And_SortingSpareParts_MATKL);
                                        break;
                                    case "SPINNING-SPINNING SPARE PARTS":
                                        object.put("Matkl", Util.SpinningSpinningSpareParts_MATKL);
                                        break;
                                    case "SPINNING-CARDING SPARE PARTS":
                                        object.put("Matkl", Util.SpinningCardingSpareParts_MATKL);
                                        break;
                                    case "SPINNING-DYEING AND MIXING SPARE PARTS":
                                        object.put("Matkl", Util.SpinningDyeingAndMixingSpareParts_MATKL);
                                        break;
                                    case "SPINNING-WORSTED SPINNING SPARE PARTS":
                                        object.put("Matkl", Util.SpinningWorsted_Spinning_Spare_Parts_MATKL);
                                        break;
                                    case "WEAVING-WEAVING SPARE PARTS":
                                        object.put("Matkl", Util.WeavingWeaving_Spare_Parts_MATKL);
                                        break;
                                    case "WEAVING-FINISHING SPARE PARTS":
                                        object.put("Matkl", Util.WeavingFinishing_Spare_Parts_MATKL);
                                        break;
                                    case "WEAVING-PRINTING SPARE PARTS":
                                        object.put("Matkl", Util.WeavingPrinting_Spare_Parts_MATKL);
                                        break;
                                    case "KNITTING-KNITTING SPARE PARTS":
                                        object.put("Matkl", Util.KnittingKnitting_Spare_Parts_MATKL);
                                        break;
                                    case "KNITTING-LINKING AND FINISHING SPARE PARTS":
                                        object.put("Matkl", Util.KnittingLinking_And_Finishing_Spare_Parts_MATKL);
                                        break;
                                    case "KNITTING-WASHING SPARE PARTS":
                                        object.put("Matkl", Util.KnittingWashing_Spare_Parts_MATKL);
                                        break;
                                    case "SEWING SPARE PARTS":
                                        object.put("Matkl", Util.SewingSpare_Parts_MATKL);
                                        break;
                                    case "DRY CLEAN SPARE PARTS":
                                        object.put("Matkl", Util.DryClean_Spare_Parts_MATKL);
                                        break;
                                    case "STANDART SPARE PARTS":
                                        object.put("Matkl", Util.Standart_Spare_Parts_MATKL);
                                        break;
                                    case "IT SPARE PARTS":
                                        object.put("Matkl", Util.IT_Spare_Parts_MATKL);
                                        break;
                                    case "OTHER SPARE PARTS":
                                        object.put("Matkl", Util.Other_Spare_Parts_MATKL);
                                        break;
                                    case "BUILDING MATERIALS":
                                        object.put("Matkl", Util.Building_Materials_MATKL);
                                        break;
                                    case "AUXILIARY MATERIAL, BUTTON":
                                        object.put("Matkl", Util.AuxiliaryMaterial_Button_MATKL);
                                        break;
                                    case "AUXILIARY MATERIAL, BUCKLE":
                                        object.put("Matkl", Util.AuxiliaryMaterial_Buckle_MATKL);
                                        break;
                                    case "AUXILIARY MATERIAL, FUR":
                                        object.put("Matkl", Util.AuxiliaryMaterial_Fur_MATKL);
                                        break;
                                    case "AUXILIARY MATERIAL, STICKER":
                                        object.put("Matkl", Util.AuxiliaryMaterial_Sticker_MATKL);
                                        break;
                                    case "AUXILIARY MATERIAL, WING":
                                        object.put("Matkl", Util.AuxiliaryMaterial_Wing_MATKL);
                                        break;
                                    case "AUXILIARY MATERIAL, RIBBON":
                                        object.put("Matkl", Util.AuxiliaryMaterial_Ribbon_MATKL);
                                        break;
                                    case "AUXILIARY MATERIAL, SEWING THREAD":
                                        object.put("Matkl", Util.Auxiliary_Material_Sewing_Thread_MATKL);
                                        break;
                                    case "AUXILIARY MATERIAL, UNIT":
                                        object.put("Matkl", Util.AuxiliaryMaterial_Unit_MATKL);
                                        break;
                                    case "AUXILIARY MATERIAL, WOVEN":
                                        object.put("Matkl", Util.AuxiliaryMaterial_Woven_MATKL);
                                        break;
                                    case "AUXILIARY MATERIAL, ZIPPER":
                                        object.put("Matkl", Util.AuxiliaryMaterial_Zipper_MATKL);
                                        break;
                                    case "CHEMICAL, CHEMICAL":
                                        object.put("Matkl", Util.Chemical_Chemical_MATKL);
                                        break;
                                    case "CHEMICAL, DYE":
                                        object.put("Matkl", Util.Chemical_Dye_MATKL);
                                        break;
                                    case "LABEL, CARE LABEL":
                                        object.put("Matkl", Util.Label_CareLabel_MATKL);
                                        break;
                                    case "LABEL, WOVEN LABEL":
                                        object.put("Matkl", Util.Label_WovenLabel_MATKL);
                                        break;
                                    case "LABEL, LABEL":
                                        object.put("Matkl", Util.Label_Label_MATKL);
                                        break;
                                    case "OTHER, FUR":
                                        object.put("Matkl", Util.Other_Fur_MATKL);
                                        break;
                                    case "OTHER, OTHER":
                                        object.put("Matkl", Util.Other_Other_MATKL);
                                        break;
                                    case "OTHER, RIBBON":
                                        object.put("Matkl", Util.Other_Ribbon_MATKL);
                                        break;
                                    case "OTHER, OTHER THREAD":
                                        object.put("Matkl", Util.Other_OtherThread_MATKL);
                                        break;
                                    case "OTHER, UNIT":
                                        object.put("Matkl", Util.Other_Unit_MATKL);
                                        break;
                                    case "OTHER, WOVEN":
                                        object.put("Matkl", Util.Other_Woven_MATKL);
                                        break;
                                    case "PACKAGE, GRID":
                                        object.put("Matkl", Util.Package_Grid_MATKL);
                                        break;
                                    case "PACKAGE, HANGER":
                                        object.put("Matkl", Util.Package_Hanger_MATKL);
                                        break;
                                    case "PACKAGE, DAAVUU DRAP, BAGLAA":
                                        object.put("Matkl", Util.Package_Daavuu_drap);
                                        break;
                                    case "PACKAGE, ":
                                        object.put("Matkl", Util.Package_N_MATKL);
                                        break;
                                    case "PACKAGE OTHER":
                                        object.put("Matkl", Util.Package_Other_MATKL);
                                        break;
                                    case "PACKAGE PRESS":
                                        object.put("Matkl", Util.Package_Press_MATKL);
                                        break;
                                    case "PACKAGE TAPE":
                                        object.put("Matkl", Util.Package_Tape_MATKL);
                                        break;
                                    case "PACKAGE BOX":
                                        object.put("Matkl", Util.Package_Box_MATKL);
                                        break;
                                    case "PACKAGE BAG":
                                        object.put("Matkl", Util.Package_Bag_MATKL);
                                        break;
                                    case "YARN, KNITTING":
                                        object.put("Matkl", Util.Yarn_Knitting_MATKL);
                                        break;
                                    case "YARN, WOVEN":
                                        object.put("Matkl", Util.Yarn_Woven_MATKL);
                                        break;
                                    case "DRY CLEANING":
                                        object.put("Matkl", Util.Dry_Cleaning_MATKL);
                                        break;
                                    case "WET CLEANING":
                                        object.put("Matkl", Util.Wet_Cleaning_MATKL);
                                        break;
                                    case "REPAIRING":
                                        object.put("Matkl", Util.Repairing_MATKL);
                                        break;
                                    case "MADE CUSTOM":
                                        object.put("Matkl", Util.Made_Custom_MATKL);
                                        break;
                                    case "SPARE PARTS":
                                        object.put("Matkl", Util.Spare_Parts_MATKL);
                                        break;
                                    case "SERVICE":
                                        object.put("Matkl", Util.Service_MATKL);
                                        break;
                                    case "PLUMBING SERVICE":
                                        object.put("Matkl", Util.Plumbing_Service_MATKL);
                                        break;
                                    case "ELECTRICAL SERVICE":
                                        object.put("Matkl", Util.Electrical_Service_MATKL);
                                        break;
                                    case "ELECTRONIC AND TELEPHONE SERVICE":
                                        object.put("Matkl", Util.Electronic_And_Telephone_Service_MATKL);
                                        break;
                                    case "VENTILATION SERVICE":
                                        object.put("Matkl", Util.Ventilation_Service_MATKL);
                                        break;
                                    case "BUILDING SERVICE":
                                        object.put("Matkl", Util.Building_Service_MATKL);
                                        break;
                                    case "EQUIPMENT SERVICE":
                                        object.put("Matkl", Util.Equipment_Service_MATKL);
                                        break;
                                    case "EQUIPMENT RENT SERVICE":
                                        object.put("Matkl", Util.Equipment_Rent_Service_MATKL);
                                        break;
                                    case "SEMI-PRODUCT":
                                        object.put("Matkl", Util.Semi_Product_MATKL);
                                        break;
                                    case "BY-PRODUCT":
                                        object.put("Matkl", Util.By_Product_MATKL);
                                        break;
                                    case "STATIONARY MATERIAL":
                                        object.put("Matkl", Util.Stationery_Material_MATKL);
                                        break;
                                    case "CLEANING MATERIAL":
                                        object.put("Matkl", Util.Cleaning_Material_MATKL);
                                        break;
                                    case "SAFETY MATERIAL":
                                        object.put("Matkl", Util.Safety_Material_MATKL);
                                        break;
                                    case "SUPPLY OTHER MATERIAL":
                                        object.put("Matkl", Util.Supply_Other_Material_MATKL);
                                        break;
                                    case "UTILITY MATERIAL":
                                        object.put("Matkl", Util.Utility_Material_MATKL);
                                        break;
                                    default: if(plm.getMaterialGroupCategorySubCategory().length()==1 && isNumeric(plm.getMaterialGroupCategorySubCategory())){
                                        object.put("Matkl", "0"+plm.getMaterialGroupCategorySubCategory().toUpperCase());
                                    }else if(plm.getMaterialGroupCategorySubCategory().length()>9){
                                        object.put("Matkl", plm.getMaterialGroupCategorySubCategory().toUpperCase().substring(0, 9));
                                    }else{
                                        object.put("Matkl", plm.getMaterialGroupCategorySubCategory().toUpperCase());
                                    }
                                }

                                map.add(object);
                            }
                            //Spart Mandatory Switch
                            {
                                JSONObject object = new JSONObject();
                                switch (plm.getDivision().toUpperCase()) {
                                    case "GENERAL":
                                    case "":
                                        object.put("Spart", Util.General_SPART);
                                        break;
                                    case "KNIT":
                                        object.put("Spart", Util.Knitting_SPART);
                                        break;
                                    case "SEWN":
                                        object.put("Spart", Util.Sewing_SPART);
                                        break;
                                    case "WOVEN":
                                        object.put("Spart", Util.Woven_SPART);
                                        break;
                                    case "OTHER":
                                        object.put("Spart", Util.Other_SPART);
                                        break;
                                    default:
                                        if(plm.getDivision().length()>2){
                                            object.put("Spart", plm.getDivision().toUpperCase().substring(0, 2));
                                        }else{
                                            object.put("Spart", plm.getDivision().toUpperCase());
                                        }
                                }
                                map.add(object);
                            }
                            if(plm.getIndustryStandartDescriptionDepartment()!=null){
                                JSONObject object = new JSONObject();
                                if(plm.getIndustryStandartDescriptionDepartment().length()>18){
                                    object.put("Normt", plm.getIndustryStandartDescriptionDepartment().substring(0, 18));
                                }else{
                                    object.put("Normt", plm.getIndustryStandartDescriptionDepartment());
                                }

                                map.add(object);
                            }else{
                                JSONObject object = new JSONObject();
                                object.put("Normt", "");
                                map.add(object);
                            }
                            if(plm.getGrossWeight()!=null){
                                JSONObject object = new JSONObject();
                                String brgewStr = plm.getGrossWeight();
                                System.out.println("brgewStr: "+brgewStr);
                                if(brgewStr.contains(".")){
                                    String brgw = brgewStr.substring(brgewStr.indexOf("."));
                                    System.out.println("brgw.length(): "+brgw.length());
                                    if(brgw.length()>5){
                                        brgewStr = brgewStr.substring(0, brgewStr.indexOf(".")+4);
                                    }else{
                                        int brgwIndx = brgw.length()-1;
                                        brgewStr = brgewStr.substring(0, brgewStr.indexOf(".")+brgwIndx);
                                    }
                                }
                                object.put("Brgew", brgewStr);
                                map.add(object);
                            }else{
                                JSONObject object = new JSONObject();
                                object.put("Brgew", "");
                                map.add(object);
                            }
                            if(plm.getWeightUnit()!=null){
                                JSONObject object = new JSONObject();
                                if(plm.getWeightUnit().length()>3){
                                    object.put("Gewei", plm.getWeightUnit().substring(0, 3));
                                }else{
                                    object.put("Gewei", plm.getWeightUnit());
                                }
                                map.add(object);
                            }else{
                                JSONObject object = new JSONObject();
                                object.put("Gewei", "");
                                map.add(object);
                            }
                            if(plm.getNetWeight()!=null){
                                JSONObject object = new JSONObject();
                                String ntgewStr = plm.getNetWeight();
                                if(ntgewStr.contains(".")){
                                    String ntgew = ntgewStr.substring(ntgewStr.indexOf("."));
                                    System.out.println("ntgew.length(): "+ntgew.length());
                                    if(ntgew.length()>5){
                                        ntgewStr = ntgewStr.substring(0, ntgewStr.indexOf(".")+4);
                                    }else{
                                        int ntgw = ntgew.length()-1;
                                        ntgewStr = ntgewStr.substring(0, ntgewStr.indexOf(".")+ntgw);
                                    }
                                }
                                object.put("Ntgew", ntgewStr);
                                map.add(object);
                            }else{
                                JSONObject object = new JSONObject();
                                object.put("Ntgew", "");
                                map.add(object);
                            }
                            if(plm.getExternalMaterialGroupBrand()!=null){
                                JSONObject object = new JSONObject();
                                if(plm.getExternalMaterialGroupBrand().length()>18){
                                    object.put("Extwg", plm.getExternalMaterialGroupBrand().substring(0, 18));
                                }else{
                                    object.put("Extwg", plm.getExternalMaterialGroupBrand());
                                }

                                map.add(object);
                            }else{
                                JSONObject object = new JSONObject();
                                object.put("Extwg","");
                                map.add(object);
                            }
                            {
                                JSONObject object = new JSONObject();
                                object.put("MtposMara","NORM");
                                map.add(object);
                            }
                            if(plm.getBasicMaterialStyle()!=null){
                                JSONObject object = new JSONObject();
                                if(plm.getBasicMaterialStyle().length()>48){
                                    object.put("Wrkst", plm.getBasicMaterialStyle().substring(0, 48));
                                }else{
                                    object.put("Wrkst", plm.getBasicMaterialStyle());
                                }
                                map.add(object);
                            }else{
                                JSONObject object = new JSONObject();
                                object.put("Wrkst", "");
                                map.add(object);
                            }
                            if(plm.getMaterialLongDescription()!=null){
                                JSONObject object = new JSONObject();
//                                String code = plm.getMaterialCode().substring(0, 2);
                                String code = plm.getMaterialGroupCategorySubCategory();
                                String longDesc;
                                switch (code){
                                    case "AB":
                                    case "AD":
                                    case "AF":
                                    case "AK":
                                    case "AW":
                                    case "OJ":
                                    case "AH":
                                    case "AI":
                                    case "AZ":
                                    case "AR":
                                    case "OO":
                                    case "AS":
                                    case "LC":
                                    case "LE":
                                    case "LL":
                                    case "OT":
                                    case "OU":
                                    case "PG":
                                    case "PJ":
                                    case "PM":
                                    case "PN":
                                    case "PO":
                                    case "PP":
                                    case "PV":
                                    case "PX":
                                    case "PY":
                                    case "PC":
                                    case "PF":
                                    case "PH":
                                    case "PI":
                                    case "PS":
                                    case "PZ":
                                    case "CC":
                                    case "CD":
                                    case "YK":
                                    case "YW":
                                    case "WF": longDesc = plm.getMaterialName()+", "+plm.getMaterialDescription()+", "+plm.getAdditionalInformation()+", "+plm.getSize()+", "+plm.getColor()+", "+plm.getMainMaterialComposition();
                                    break;
                                    default: longDesc = plm.getMaterialLongDescription();
                                    break;
                                }
                                if(longDesc.length()>200){
                                    longDesc = longDesc.substring(0, 200);
                                }
                                object.put("MaterialDescriptionText", longDesc);
                                plm.setSapMaterialLongDescription(longDesc);
                                map.add(object);
                            }else{
                                JSONObject object = new JSONObject();
                                object.put("MaterialDescriptionText", "");
                                map.add(object);
                            }
                            if(plm.getSize()!=null){
                                JSONObject object = new JSONObject();
                                if(plm.getSize().length()>70){
                                    object.put("Z001", plm.getSize().substring(0, 70));
                                }else{
                                    object.put("Z001", plm.getSize());
                                }
                                map.add(object);
                            }else{
                                JSONObject object = new JSONObject();
                                object.put("Z001", "");
                                map.add(object);
                            }
                            if(plm.getColor()!=null){
                                JSONObject object = new JSONObject();
                                if(plm.getColor().length()>70){
                                    object.put("Z002", plm.getColor().substring(0, 70));
                                }else{
                                    object.put("Z002", plm.getColor());
                                }
                                map.add(object);
                            }else{
                                JSONObject object = new JSONObject();
                                object.put("Z002", "");
                                map.add(object);
                            }
                            if(plm.getSeason()!=null){
                                JSONObject object = new JSONObject();
                                if(plm.getSeason().length()>70){
                                    object.put("Z003", plm.getSeason().substring(0, 70));
                                }else{
                                    object.put("Z003", plm.getSeason());
                                }
                                map.add(object);
                            }else{
                                JSONObject object = new JSONObject();
                                object.put("Z003", "");
                                map.add(object);
                            }
                            if(plm.getCollection()!=null){
                                JSONObject object = new JSONObject();
                                if(plm.getCollection().length()>70){
                                    object.put("Z004", plm.getCollection().substring(0, 70));
                                }else{
                                    object.put("Z004", plm.getCollection());
                                }
                                map.add(object);
                            }else{
                                JSONObject object = new JSONObject();
                                object.put("Z004", "");
                                map.add(object);
                            }
                            if(plm.getGauge()!=null){
                                JSONObject object = new JSONObject();
                                if(plm.getGauge().length()>70){
                                    object.put("Z005", plm.getGauge().substring(0, 70));
                                }else{
                                    object.put("Z005", plm.getGauge());
                                }
                                map.add(object);
                            }else{
                                JSONObject object = new JSONObject();
                                object.put("Z005", "");
                                map.add(object);
                            }
                            if(plm.getPattern()!=null){
                                JSONObject object = new JSONObject();
                                if(plm.getPattern().length()>70){
                                    object.put("Z006", plm.getPattern().substring(0, 70));
                                }else{
                                    object.put("Z006", plm.getPattern());
                                }
                                map.add(object);
                            }else{
                                JSONObject object = new JSONObject();
                                object.put("Z006", "");
                                map.add(object);
                            }
                            if(plm.getMainMaterialComposition()!=null){
                                JSONObject object = new JSONObject();
                                if(plm.getMainMaterialComposition().length()>70){
                                    object.put("Z007", plm.getMainMaterialComposition().substring(0, 70));
                                }else{
                                    object.put("Z007", plm.getMainMaterialComposition());
                                }
                                map.add(object);
                            }else{
                                JSONObject object = new JSONObject();
                                object.put("Z007", "");
                                map.add(object);
                            }
                            if(plm.getYarnCount()!=null){
                                JSONObject object = new JSONObject();
                                if(plm.getYarnCount().length()>70){
                                    object.put("Z008", plm.getYarnCount().substring(0, 70));
                                }else{
                                    object.put("Z008", plm.getYarnCount());
                                }
                                map.add(object);
                            }else{
                                JSONObject object = new JSONObject();
                                object.put("Z008", "");
                                map.add(object);
                            }
                            {
                                JSONObject object = new JSONObject();
                                object.put("Z009", "");
                                map.add(object);
                            }
                            {
                                JSONObject object = new JSONObject();
                                object.put("Z010", "");
                                map.add(object);
                            }
                            if(plm.getOnlineCode()!=null){
                                JSONObject object = new JSONObject();
                                if(plm.getOnlineCode().length()>70){
                                    object.put("Z011", plm.getOnlineCode().substring(0, 70));
                                }else{
                                    object.put("Z011", plm.getOnlineCode());
                                }
                                map.add(object);
                            }else{
                                JSONObject object = new JSONObject();
                                object.put("Z001", "");
                                map.add(object);
                            }
                            if(plm.getColorName()!=null){
                                JSONObject object = new JSONObject();
                                if(plm.getColorName().length()>70){
                                    object.put("Z012", plm.getColorName().substring(0, 70));
                                }else{
                                    object.put("Z012", plm.getColorName());
                                }
                                map.add(object);
                            }else{
                                JSONObject object = new JSONObject();
                                object.put("Z012", "");
                                map.add(object);
                            }
                            if(plm.getKidsClassification()!=null){
                                JSONObject object = new JSONObject();
                                if(plm.getKidsClassification().length()>70){
                                    object.put("Z013", plm.getKidsClassification().substring(0, 70));
                                }else{
                                    object.put("Z013", plm.getKidsClassification());
                                }
                                map.add(object);
                            }else{
                                JSONObject object = new JSONObject();
                                object.put("Z013", "");
                                map.add(object);
                            }

                            String stringJson = map.toString();
                            stringJson = stringJson.replace("{", "");
                            stringJson = stringJson.replace("}", "");
                            stringJson = stringJson.replace("]", "}");
                            stringJson = stringJson.replace("[", "{");
                            itemList.append(", ").append(stringJson);
                        }
                if(counter%50==0 && itemList.length()>2){
                    String theString = itemList.substring(2);

                    theString = "{\n" +
                            "    \"IInfo\": {\n" +
                            "    \"Source\": \"SOAPUI\", \n" +
                            "    \"Destination\": \"SAP\", \n" +
                            "    \"Zdate\": \""+zDate+"\", \n" +
                            "    \"Ztime\": \"" +zTime+ "\" \n" +
                            "    },\n" +
                            "    \"ItData\": {\n" +
                            "        \"item\": ["+theString+"]\n" +
                            "    }\n" +
                            "}";
                    System.out.println("theString: "+theString);
                    postToSAP(theString);
                    counter=0;
                    itemList = new StringBuilder();
                }
                else if (csvRecords.size()==generalCounter && itemList.length()>2) {
                    String theString = itemList.substring(2);

                    theString = "{\n" +
                            "    \"IInfo\": {\n" +
                            "    \"Source\": \"SOAPUI\", \n" +
                            "    \"Destination\": \"SAP\", \n" +
                            "    \"Zdate\": \""+zDate+"\", \n" +
                            "    \"Ztime\": \"" +zTime+ "\" \n" +
                            "    },\n" +
                            "    \"ItData\": {\n" +
                            "        \"item\": ["+theString+"]\n" +
                            "    }\n" +
                            "}";
                    System.out.println("theString: "+theString);
                    postToSAP(theString);
                }
            }
//            String theString = itemList.substring(2);
//            theString = "{\n" +
//                    "    \"IInfo\": {\n" +
//                    "    \"Source\": \"SOAPUI\", \n" +
//                    "    \"Destination\": \"SAP\", \n" +
//                    "    \"Zdate\": \""+zDate+"\", \n" +
//                    "    \"Ztime\": " +zTime+ " \n" +
//                    "    },\n" +
//                    "    \"ItData\": {\n" +
//                    "        \"item\": ["+theString+"]\n" +
//                    "    }\n" +
//                    "}";
//            System.out.println("theString: "+theString);
//            postToSAP(theString);
            return plms;
        } catch (IOException e) {
            throw new RuntimeException("fail to parse CSV file: " + e.getMessage());
        }
    }

    public static void postToSAP(String plmPostDTO) {

        HttpEntity<String> request = new HttpEntity<>(plmPostDTO, Util.internalPostHeaders());
        ResponseEntity<String> responseEntity;

        final RestTemplate restTemplate = new RestTemplate();

        try{
//            responseEntity = restTemplate.exchange(Util.plmPostUrlToSAP, HttpMethod.POST, request, String.class);
            responseEntity = restTemplate.exchange(Util.prodPlmPostUrlToSAP, HttpMethod.POST, request, String.class);
            String result = responseEntity.getBody();
            System.out.println("result: "+result);
            JSONParser parser = new JSONParser();
            parser.parse(result);
        }catch (Exception e){
            System.out.println("rest template exception: "+e);
        }
    }

    public static boolean isNumeric(String strNum) {
        if (strNum == null) {
            return false;
        }
        try {
            Double.parseDouble(strNum);
        } catch (NumberFormatException nfe) {
            return false;
        }
        return true;
    }

    }
