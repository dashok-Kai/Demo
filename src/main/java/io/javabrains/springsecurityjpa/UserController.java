package io.javabrains.springsecurityjpa;

import com.amazonaws.AmazonServiceException;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.util.IOUtils;
import io.javabrains.springsecurityjpa.models.User;
import io.javabrains.springsecurityjpa.models.UserPic;
import org.apache.tomcat.util.http.fileupload.FileUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletRequest;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.sql.Timestamp;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@RestController
public class UserController {

    @Autowired
    UserRepository userRepository;

    @Autowired
    ImageRepository imageRepository;

    @Autowired
    private AmazonS3 amazonS3;

    //amazonS3 = AmazonS3ClientBuilder.standard().withRegion(Regions.DEFAULT_REGION).build();

    @Value("${bucketName}")
    private String bucket;
    //private String bucketURL="https://s3.console.aws.amazon.com/s3/buckets/csye6225.prod.domain.tld?region=us-east-1&tab=objects";

//    private String bucket = "csye6225.prod.domain.tld";
//    private String bucketURL = "https://s3.console.aws.amazon.com/s3/buckets/csye6225.prod.domain.tld?region=us-east-1&tab=objects";

//    @GetMapping("/")
//    public String home() {
//        return ("<h1>Welcome</h1>");
//    }

    @GetMapping("/v1/user/self")
    public ResponseEntity<User> user(Authentication authentication) {
        User user = userRepository.findByUserName(authentication.getName())
                .orElseThrow(() -> new ResourceNotFoundException("Employee not exist with id:" + authentication.getName()));
        return ResponseEntity.ok(user);
    }

    // build update user REST API
    @PutMapping("/v1/user/self")
    public ResponseEntity<User> updateUser(Authentication authentication, @RequestBody User userDetails) {
        try {
            if (userDetails.getUserName() != null && !userDetails.getUserName().isEmpty()) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(null);
            }

            if (userDetails.getAccountCreated() != null) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(null);
            }

            if (userDetails.getAccountUpdated() != null) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(null);
            }

            if ((userDetails.getFirstName() != null && !userDetails.getFirstName().isEmpty())
                    && (userDetails.getLastName() != null && !userDetails.getLastName().isEmpty()) &&
                    (userDetails.getPassword() != null && !userDetails.getPassword().isEmpty())) {
                String password = userDetails.getPassword();
                BCryptPasswordEncoder bCryptPasswordEncoder = new BCryptPasswordEncoder();
                userDetails.setPassword(bCryptPasswordEncoder.encode(password));
                userDetails.setAccountUpdated(new Timestamp(System.currentTimeMillis()));
                //System.out.println(">>>>>>>>>>>>>>>>>Pass -" + userDetails.getPassword());
                userRepository.updateUser(authentication.getName(), userDetails.getFirstName(),
                        userDetails.getLastName(), userDetails.getPassword(), userDetails.getAccountUpdated());
                return ResponseEntity.status(HttpStatus.NO_CONTENT).body(null);
            }

            if (userDetails.getFirstName() != null && !userDetails.getFirstName().isEmpty()) {
                userDetails.setAccountUpdated(new Timestamp(System.currentTimeMillis()));
                userRepository.updateUserFirstName(authentication.getName(), userDetails.getFirstName(), userDetails.getAccountUpdated());
            }
            if (userDetails.getLastName() != null && !userDetails.getLastName().isEmpty()) {
                userDetails.setAccountUpdated(new Timestamp(System.currentTimeMillis()));
                userRepository.updateUserLastName(authentication.getName(), userDetails.getLastName(), userDetails.getAccountUpdated());
            }

            if (userDetails.getPassword() != null && !userDetails.getPassword().isEmpty()) {
                String password = userDetails.getPassword();
                BCryptPasswordEncoder bCryptPasswordEncoder = new BCryptPasswordEncoder();
                userDetails.setPassword(bCryptPasswordEncoder.encode(password));
                //System.out.println(">>>>>>>>>>>>>>>>>Pass -" + userDetails.getPassword());
                userRepository.updateUserPassword(authentication.getName(), userDetails.getPassword(), userDetails.getAccountUpdated());
            }
            return ResponseEntity.status(HttpStatus.NO_CONTENT).body(null);
        } catch (Exception exception) {
            exception.printStackTrace();
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(null);
        }

    }

//    private ResponseEntity<User> getUserDetails(Authentication authentication) {
//        User user = userRepository.findByUserName(authentication.getName())
//                .orElseThrow(() -> new ResourceNotFoundException("Employee not exist with id:" + authentication.getName()));
//        return ResponseEntity.ok(user);
//    }


    @PostMapping("/v1/user")
    public ResponseEntity<User> registerUser(@RequestBody User newUser) {
        try {
            System.out.println("PosT=======Entered=============================");
            if (!isValidEmailAddress(newUser.getUserName())) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(null);
            }
            System.out.println("PosT=====================================");
            List<User> users = userRepository.findAll();
            //System.out.println("New user: " + newUser.toString());
            for (User user : users) {
                //System.out.println("Registered user: " + newUser.toString());
                if (user.getUserName().equals(newUser.getUserName())) {
                    // System.out.println("User Already exists!");
                    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(null);
                }
            }
            BCryptPasswordEncoder bCryptPasswordEncoder = new BCryptPasswordEncoder();
            newUser.setPassword(bCryptPasswordEncoder.encode(newUser.getPassword()));
            if (saveDetail(newUser, userRepository)) {
                User user = userRepository.findByUserName(newUser.getUserName())
                        .orElseThrow(() -> new ResourceNotFoundException("Employee not exist with id:" + newUser.getUserName()));
                return ResponseEntity.status(HttpStatus.CREATED).body(user);
            } else {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(null);
            }
        } catch (Exception e){
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(null);
        }
    }

    public boolean isValidEmailAddress(String email) {
        String ePattern = "^[a-zA-Z0-9.!#$%&'*+/=?^_`{|}~-]+@((\\[[0-9]{1,3}\\.[0-9]{1,3}\\.[0-9]{1,3}\\.[0-9]{1,3}\\])|(([a-zA-Z\\-0-9]+\\.)+[a-zA-Z]{2,}))$";
        java.util.regex.Pattern p = java.util.regex.Pattern.compile(ePattern);
        java.util.regex.Matcher m = p.matcher(email);
        return m.matches();
    }


    public static boolean saveDetail(User newUser, UserRepository userRepository) {
        try {
            newUser.setId(UUID.randomUUID());
            newUser.setAccountCreated(new Timestamp(System.currentTimeMillis()));
            newUser.setAccountUpdated(new Timestamp(System.currentTimeMillis()));
            newUser.setActive(true);
            // System.out.println(System.currentTimeMillis());

            userRepository.save(newUser);
            return true;
        } catch (Exception exception) {
            exception.printStackTrace();
            return false;
        }
    }

    @PostMapping("/v1/user/self/pic")
    public ResponseEntity addUpdatePic(Authentication authentication, HttpServletRequest request, @RequestParam(value = "profilePic") MultipartFile multipartFile) {
        try {
        HttpServletRequest httpServletRequest = request;
            byte[] processedFile = IOUtils.toByteArray(request.getInputStream());
            File newFile = new File("abc");
//        httpServletRequest.getAttributeNames();
        //Path path = request.getPart();
       // path.getFileName();
        //convert byte[] and then to file and then to s3 bucket
        //FileUtils.writeByteArrayToFile(new File("pathname"), myByteArray)
        String fileUrl = "";
        String fileName = multipartFile.getOriginalFilename();
        Timestamp updateDate = new Timestamp(System.currentTimeMillis());

        User user = userRepository.findByUserName(authentication.getName())
                .orElseThrow(() -> new ResourceNotFoundException("Employee not exist with id:" + authentication.getName()));
        UserPic picData = imageRepository.getUserData(user.getId().toString());
        imageRepository.flush();
            File file = convertMultiPartToFile(multipartFile);
            String fileNameWithDate = new Date().getTime() + "-" + fileName.replace(" ", "_");
//            fileUrl = bucketURL+"/"+bucket+"/"+fileNameWithDate;
            String userID = user.getId().toString();
            fileUrl = userID + "/" + fileNameWithDate;

            if (picData != null) {
                amazonS3.deleteObject(bucket, picData.getUrl());
//                picData.setUploadDate(new Timestamp(System.currentTimeMillis()));
//                picData.setFileName(fileNameWithDate);
//                picData.setUrl(fileUrl);
                System.out.println(">>>>>>>>>>>>>>>>>>>>>>>>> Update Pic");
                amazonS3.putObject(bucket, fileUrl, file);
                imageRepository.updatePic(userID, fileNameWithDate, fileUrl, new Timestamp(System.currentTimeMillis()));
                 picData = imageRepository.getUserData(user.getId().toString());
                return new ResponseEntity<>(picData, HttpStatus.CREATED);
            } else {
                System.out.println(">>>>>>>>>>>>>>>>>>>>>>>>> New Pic");
                picData = new UserPic(fileNameWithDate, fileUrl, updateDate, user.getId().toString());
            }
            amazonS3.putObject(bucket, fileUrl, file);
            System.out.println(">>>>>>>>>>>>>>>>>>>>>>>>>>>>" + picData + "<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<");
            return new ResponseEntity<>(imageRepository.save(picData), HttpStatus.CREATED);
        }catch (Exception e){
        e.printStackTrace();
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(null);
    }
        }

        @GetMapping("/v1/user/self/pic")
            public ResponseEntity getPic(Authentication authentication){
            try{
                User user = userRepository.findByUserName(authentication.getName())
                        .orElseThrow(() -> new ResourceNotFoundException("Employee not exist with id:" + authentication.getName()));
                UserPic picData = imageRepository.findByUserId(user.getId().toString());
                    if(picData != null)
                        return new ResponseEntity<>(picData,HttpStatus.OK);

                    return new ResponseEntity<>(HttpStatus.NOT_FOUND);
            }
            catch (Exception e){
                return new ResponseEntity<>(null,HttpStatus.BAD_REQUEST);
            }
    }

    @DeleteMapping("/v1/user/self/pic")
            public ResponseEntity deletePic(Authentication authentication){
                User user = userRepository.findByUserName(authentication.getName())
                .orElseThrow(() -> new ResourceNotFoundException("Employee not exist with id:" + authentication.getName()));
       // UserPic picData = imageRepository.findByUserId(user.getId().toString());
        try{

                UserPic imageData = imageRepository.findByUserId(user.getId().toString());

                if (imageData != null) {
                    System.out.println(">>>>>>>>>>>>>>>>>>> Delete - user - "+ imageData.getUser_id());
                    amazonS3.deleteObject(bucket, imageData.getUrl());
                    imageRepository.deleteByUserId(imageData.getUser_id());
                    return new ResponseEntity<>(HttpStatus.OK);
                }
                return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
        catch (Exception e){
            e.printStackTrace();
            return new ResponseEntity<>(null,HttpStatus.BAD_REQUEST);
        }
    }


    private File convertMultiPartToFile(MultipartFile  file) throws IOException {
        File convertedFile = new File(file.getOriginalFilename());
        FileOutputStream fos = new FileOutputStream(convertedFile);
        fos.write(file.getBytes());
        fos.close();
        return convertedFile;

    }
}

