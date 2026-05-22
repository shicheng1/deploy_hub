package com.deployhub.service;

import com.deployhub.common.PageResult;
import com.deployhub.common.Result;
import com.deployhub.vo.NginxPackageVO;
import org.springframework.web.multipart.MultipartFile;

public interface NginxPackageService {

    Result<PageResult<NginxPackageVO>> list(int pageNum, int pageSize, String name);

    Result<NginxPackageVO> getById(Long id);

    /**
     * 上传Nginx安装包
     *
     * @param file        安装包文件
     * @param name        包名称
     * @param version     版本号
     * @param description 描述
     * @return 上传结果
     */
    Result<Void> upload(MultipartFile file, String name, String version, String description);

    Result<Void> delete(Long id);

    /**
     * 将安装包安装到指定服务器
     *
     * @param packageId 安装包ID
     * @param serverId  目标服务器ID
     * @return 安装结果输出
     */
    Result<String> installToServer(Long packageId, Long serverId);
}
