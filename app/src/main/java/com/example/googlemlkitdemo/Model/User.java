package com.example.googlemlkitdemo.Model;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

public class User {

    @SerializedName("id_pessoa_fisica")
    @Expose
    private int idPessoaFisica;
    @SerializedName("id_empresa")
    @Expose
    private int idEmpresa;
    @SerializedName("nome")
    @Expose
    private String nome;
    @SerializedName("foto")
    @Expose
    private String foto;

    public User() {
    }

    public User (int idPessoaFisica, int idEmpresa, String nome, String foto) {
        this.idPessoaFisica = idPessoaFisica;
        this.idEmpresa = idEmpresa;
        this.nome = nome;
        this.foto = foto;
    }

    public int getIdPessoaFisica() {
        return idPessoaFisica;
    }

    public void setIdPessoaFisica(int idPessoaFisica) {
        this.idPessoaFisica = idPessoaFisica;
    }

    public int getIdEmpresa() {
        return idEmpresa;
    }

    public void setIdEmpresa(int idEmpresa) {
        this.idEmpresa = idEmpresa;
    }

    public String getNome() {
        return nome;
    }

    public void setNome(String nome) {
        this.nome = nome;
    }

    public String getFoto() {
        return foto;
    }

    public void setFoto(String foto) {
        this.foto = foto;
    }
}
