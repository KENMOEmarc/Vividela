package com.template.vivid.service;

import com.template.vivid.model.dto.RegisterRequest;
import com.template.vivid.model.dto.UserFormRequest;
import com.template.vivid.model.dto.UserDto;

import java.util.List;

/**
 * Interface du service utilisateur.
 * <p>
 * Expose les opérations CRUD complètes sur les utilisateurs,
 * utilisées à la fois par l'API d'authentification et par le dashboard admin.
 */
public interface UserService {

    /**
     * Crée un utilisateur via le formulaire d'inscription public.
     */
    UserDto register(RegisterRequest request);

    /**
     * Retourne la liste de tous les utilisateurs (admin).
     */
    List<UserDto> findAll();

    UserDto findByUserName(String userName);

    /**
     * Recherche un utilisateur par son email.
     */
    UserDto findByEmail(String email);


    /**
     * Recherche un utilisateur par son ID.
     */
    UserDto findById(Long id);

    /**
     * Crée un utilisateur depuis le dashboard admin (sans confirmPassword).
     *
     * @param request       données du formulaire
     * @param currentUserId ID de l'utilisateur actuellement authentifié (le "créateur"),
     *                      résolu côté contrôleur depuis le token JWT — JAMAIS depuis
     *                      un champ du corps de la requête (non fiable / falsifiable).
     */
    UserDto create(UserFormRequest request, Long currentUserId);

    /**
     * Met à jour les informations d'un utilisateur existant.
     *
     * @param id            utilisateur à modifier
     * @param request       données du formulaire
     * @param currentUserId ID de l'utilisateur actuellement authentifié, résolu
     *                      côté contrôleur depuis le token JWT — utilisé pour vérifier
     *                      que seul un ADMIN peut attribuer/conserver le rôle ADMIN.
     */
    UserDto update(Long id, UserFormRequest request, Long currentUserId);

    /**
     * Supprime un utilisateur par son ID.
     *
     * @param id            utilisateur à supprimer
     * @param currentUserId ID de l'utilisateur actuellement authentifié, résolu
     *                      côté contrôleur depuis le token JWT — utilisé pour
     *                      empêcher l'auto-suppression (voir UserServiceImpl).
     */
    void delete(Long id, Long currentUserId);

    List<UserDto> findAllCustomer();

    /**
     * Crée un client (rôle CUSTOMER) depuis la page "Clients" du dashboard admin.
     * <p>
     * BUGFIX : le frontend (CustomerFormModal) n'envoie que firstName/lastName/email/phone
     * et appelait POST /customers, qui n'existait sur aucun contrôleur backend → 404
     * systématique. Le nom d'utilisateur et le mot de passe (requis par le modèle User)
     * sont générés automatiquement ici.
     */
    UserDto createCustomer(UserFormRequest request, Long currentUserId);

    /**
     * Met à jour les informations (nom, email, téléphone) d'un client existant.
     */
    UserDto updateCustomerInfo(Long id, UserFormRequest request);
}