package com.zmbdp.mstemplate.service.test;

import com.zmbdp.mstemplate.service.domain.dto.ValidationUserReqDTO;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * 参数校验测试控制器
 * 测试 @Validated 注解的各种使用场景
 *
 * @author 稚名不带撇
 */
@Validated
@RestController
@RequestMapping("/test/validated")
public class TestValidatedController {

    /**
     * 测试 @RequestBody 参数校验
     *
     * @param userDto 用户DTO
     * @return 结果
     */
    @PutMapping("a1")
    public int a1(@RequestBody @Validated ValidationUserReqDTO userDto) {
        System.out.println(userDto);
        return 0;
    }

    /**
     * 测试普通对象参数校验
     *
     * @param userDTO 用户DTO
     * @return 结果
     */
    @DeleteMapping("/a2")
    public int a2(@Validated ValidationUserReqDTO userDTO) {
        System.out.println(userDTO);
        return 0;
    }

    /**
     * 测试单个参数校验
     *
     * @param name 名称
     * @param id   ID
     * @return 结果
     */
    @DeleteMapping("/a3")
    public int a3(@NotNull(message = "昵称不能为空") String name,
                  @Min(value = 0, message = "id不能小于0") @Max(value = 60, message = "id不能大于60") int id) {
        return 0;
    }

    /**
     * 测试路径参数校验
     *
     * @param id   ID
     * @param name 名称
     * @return 结果
     */
    @DeleteMapping("/a4/{id}/{name}")
    public int a4(@Max(value = 60, message = "id不能大于60") @PathVariable("id") Integer id,
                  @Size(min = 5, max = 10, message = "name长度不能少于5位，不能大于10位") @PathVariable("name") String name) {
        System.out.println(id);
        System.out.println(name);
        return 0;
    }
}