package com.deployhub.controller;

import com.deployhub.common.PageResult;
import com.deployhub.common.Result;
import com.deployhub.dto.ServerDTO;
import com.deployhub.service.ServerService;
import com.deployhub.vo.ServerVO;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/server")
public class ServerController {

    @Autowired
    private ServerService serverService;

    @GetMapping("/list")
    public Result<PageResult<ServerVO>> list(@RequestParam(defaultValue = "1") int pageNum,
                       @RequestParam(defaultValue = "10") int pageSize,
                       @RequestParam(required = false) String name,
                       @RequestParam(required = false) String groupName) {
        return serverService.list(pageNum, pageSize, name, groupName);
    }

    @GetMapping("/{id}")
    public Result<ServerVO> getById(@PathVariable Long id) {
        return serverService.getById(id);
    }

    @PostMapping
    public Result<Void> add(@Valid @RequestBody ServerDTO dto) {
        return serverService.add(dto);
    }

    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Long id, @Valid @RequestBody ServerDTO dto) {
        dto.setId(id);
        return serverService.update(dto);
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        return serverService.delete(id);
    }

    @GetMapping("/{id}/test-connection")
    public Result<Boolean> testConnection(@PathVariable Long id) {
        return serverService.testConnection(id);
    }
}
