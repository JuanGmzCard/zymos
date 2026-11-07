package com.alera.dto;

public class MensajeChatDto {

    private String role;      // "user" | "assistant"
    private String contenido;
    private boolean modoMock;

    public MensajeChatDto() {}

    public MensajeChatDto(String role, String contenido, boolean modoMock) {
        this.role      = role;
        this.contenido = contenido;
        this.modoMock  = modoMock;
    }

    public String  getRole()       { return role; }
    public void    setRole(String v)       { this.role = v; }

    public String  getContenido()  { return contenido; }
    public void    setContenido(String v)  { this.contenido = v; }

    public boolean isModoMock()    { return modoMock; }
    public void    setModoMock(boolean v)  { this.modoMock = v; }
}
