package com.asjy.controller;

import com.asjy.pojo.User;
import com.asjy.service.UserService;
import com.asjy.utils.R;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * ClassName: UserController
 * Package: com.asjy.controller
 * Description:
 *
 * @Author 梓维李
 * @Create 2023/2/26 15:58
 * @Version 2.0
 */
@RestController
@RequestMapping("/user")
public class UserController {

    @Autowired
    private UserService userService ;

    //查询所有
    @GetMapping
    public R selectAll(){
        List<User> list = userService.list();
        System.out.println("11111");
        System.out.println(2222);
        System.out.println(3333);
        System.out.println(444);
        System.out.println("hot-fix 修改");
        System.out.println("merge 修改 合并冲突问题-------------");
        System.out.println("hot-fix 第二次修改 冲突问题-----------");
        System.out.println("push test");
        System.out.println("GitHub update-----------");
        System.out.println("本地代码改动----------");
         System.out.println("GitHub update-----------");
        System.out.println("远程代码改动--------");
        System.out.println("本地代码再次改动------");
        System.out.println("本地代码再次再次---------改动------");
        System.out.println("远程代码再次再次---------改动--------");
         System.out.println("改动--------");
        return R.success(list) ;
    }

    //根据用户id查询
    @GetMapping("userbyid")
    public R selectbyid(@RequestParam Integer id){
        System.out.println("传过来id"+id);
        User byId = userService.getById(id);
        return R.success(byId);
    }

    @PostMapping("updateuserbyid")
    public R updatebyid(@RequestBody User user){
        user.setAddtime(new Date());
        return R.success(userService.updateById(user)) ;
    }

    @DeleteMapping
    public R deletebyid(Integer id){
        System.out.println("删除用户id为----"+id);
        return R.success(userService.removeById(id)) ;
    }
}
