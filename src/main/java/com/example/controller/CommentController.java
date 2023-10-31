package com.example.controller;

import com.example.common.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.UUID;

@RestController
@RequestMapping("/common")
@Slf4j
public class CommentController {
    @Value("${reggie.path}")
    private String basePath;
    @PostMapping("/upload")
    public Result<String> upload(MultipartFile file){//必须和前端规定的名字保持一致
        //file是一个临时文件,在这里需要设置保存到指定位置,否则请求结束后临时文件就会被删除
        log.info(file.toString());
        String suffix = file.getOriginalFilename().substring(file.getOriginalFilename().lastIndexOf("."));
        String shortFileName = UUID.randomUUID()+suffix;
        String fileName=basePath+shortFileName;
        File dir = new File(basePath);
        if(!dir.exists()){
            dir.mkdirs();
        }
        try {
            //将临时文件转存到指定文件
            //使用UUID生成唯一的文件名
            file.transferTo(new File(fileName));
        } catch (IOException e) {
            e.printStackTrace();
        }
        return Result.success(shortFileName);
    }

    /**
     * 文件下载功能
     * @param name
     * @param response
     */
    @GetMapping("/download")
    public void download(String name, HttpServletResponse response){
        //下载指定文件
        //通过输入流来读取文件内容
        log.info(name);

        try {
            FileInputStream fileInputStream = new FileInputStream(basePath+name);
            byte[] bytes = new byte[1024];
            int len = 0;
            response.setContentType("image/jpeg");
            while((len = fileInputStream.read(bytes))!= -1){
                response.getOutputStream().write(bytes,0,len);
                response.getOutputStream().flush();
            }
            response.getOutputStream().close();
            fileInputStream.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        //通过输出流来写回浏览器,在浏览器中展示图片
    }


}
