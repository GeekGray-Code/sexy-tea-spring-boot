package sexy.tea.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.multipart.MultipartHttpServletRequest;
import sexy.tea.model.Selection;
import sexy.tea.model.common.Result;
import sexy.tea.model.dto.MinioDto;
import sexy.tea.service.SelectionService;

import javax.servlet.http.HttpServletRequest;

/**
 * @author 大大大西西瓜皮🍉
 * @date 20:40 2020-09-26
 * description:
 */
@RestController
@RequestMapping("/selection")
@Slf4j
public class SelectionController {

    private final SelectionService service;

    @Autowired
    public SelectionController(SelectionService service) {
        this.service = service;
    }

    @GetMapping("/items/{pageNum}/{pageSize}")
    public Result items(@PathVariable("pageNum") int pageNum, @PathVariable("pageSize") int pageSize) {
        return service.find(pageNum, pageSize);
    }

    @GetMapping("/itemsById/{id}")
    public Result itemsById(@PathVariable("id") Integer id) {
        return service.findByPrimaryKey(id);
    }

    @PostMapping("/save")
    public Result save(@RequestBody Selection selection) {
        return service.saveOrUpdate(selection);
    }

    @PutMapping("/update")
    public Result update(@RequestBody Selection selection) {
        return service.saveOrUpdate(selection);
    }

    @DeleteMapping("/delete/{id}")
    public Result delete(@PathVariable Integer id) {
        return service.delete(id);
    }

    @PostMapping("/upload")
    public Result upload(HttpServletRequest request) {
        // 转换为MultipartHttpServletRequest
        if (request instanceof MultipartHttpServletRequest) {

            MultipartHttpServletRequest multipartHttpServletRequest = (MultipartHttpServletRequest) request;
            // 通过表单中的参数来接收文件流（可用 file.getInputStream() 来接收输入流）
            MultipartFile file = multipartHttpServletRequest.getFile("file");
            String originalFilename = file.getOriginalFilename();
            long size = file.getSize();

            log.info("上传文件的名称: {}", originalFilename);
            log.info("上传文件的大小: {}", size);

            String name = multipartHttpServletRequest.getParameter("name");
            String content = multipartHttpServletRequest.getParameter("content");
            // 获取饮料ID
            String productId = multipartHttpServletRequest.getParameter("productId");

            log.info("name: {}", name);
            log.info("content: {}", content);

            MinioDto dto = MinioDto.builder()
                    .file(file)
                    .contentType("application/octet-stream")
                    .originFilename(originalFilename)
                    .suffix("")
                    .build();

            return service.uploadImage(dto, productId);
        }
        return Result.business("上传失败.");
    }
}
