package com.zmbdp.mstemplate.service.test;

import com.zmbdp.mstemplate.service.domain.ValidationUserReqDTO;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@Validated
@RestController
@RequestMapping("/test/validated")
public class TestValidatedController {

    public static void main(String[] args) {
//        Executors
//        new ThreadPoolExecutor()
    }

    //    @PostMapping("/a1")
    @PutMapping("a1")
    public int a1(@RequestBody @Validated ValidationUserReqDTO userDto) {
        System.out.println(userDto);
        return 0;
    }

    //    @GetMapping("/a2")
    @DeleteMapping("/a2")
    public int a2(@Validated ValidationUserReqDTO userDTO) {
        System.out.println(userDTO);
        return 0;
    }

    //    @GetMapping("/a3")
    @DeleteMapping("/a3")
    public int a3(@NotNull(message = "昵称不能为空") String name,
                  @Min(value = 0, message = "id不能小于0") @Max(value = 60, message = "id不能大于60") int id) {
        return 0;
    }

    //    @GetMapping("/a4/{id}/{name}")
    @DeleteMapping("/a4/{id}/{name}")
    public int a4(@Max(value = 60, message = "id不能大于60") @PathVariable("id") Integer id,
                  @Size(min = 5, max = 10, message = "name长度不能少于5位，不能大于10位") @PathVariable("name") String name) {
        System.out.println(id);
        System.out.println(name);
        return 0;
    }
}
