package com.tourya.api.config.auth.response;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequestMapping("/auth")
public class ActivationWebController {
    @GetMapping("/activar-cuenta-web") // Una URL diferente para la página web
    public String mostrarPaginaActivacion(@RequestParam("token") String token, Model model) {
        model.addAttribute("token", token);
        return "activacion-web"; // Nombre del archivo HTML (sin la extensión .html)
    }
}
