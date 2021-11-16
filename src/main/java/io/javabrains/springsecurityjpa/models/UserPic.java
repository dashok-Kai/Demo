package io.javabrains.springsecurityjpa.models;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import java.sql.Timestamp;
import java.util.UUID;

@Entity
@Table(name ="UserPic")
public class UserPic {

    @Id
    private UUID id;
    private String file_name;
    private String url;
    private Timestamp upload_date;
    private String userId;

    public UserPic(String file_name, String url, Timestamp upload_date, String user_id){
        this.id = UUID.randomUUID();
        this.file_name = file_name;
        this.url = url;
        this.upload_date = upload_date;
        this.userId = user_id;
    }

    public UserPic() {

    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getFileName() {
        return file_name;
    }

    public void setFileName(String file_name) {
        this.file_name = file_name;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public Timestamp getUploadDate() {
        return upload_date;
    }

    public void setUploadDate(Timestamp upload_date) {
        this.upload_date = upload_date;
    }

    public String getUser_id() {
        return userId;
    }

    public void setUser_id(String user_id) {
        this.userId = user_id;
    }
}
