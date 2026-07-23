package com.template.auth.service;

import com.template.auth.model.dto.RegisterRequest;
import com.template.auth.model.dto.UserFormRequest;
import com.template.auth.model.entity.User;

import java.util.List;

/**
 * Interface du service utilisateur.
 *
 * Expose les opérations CRUD complètes sur les utilisateurs,
 * utilisées à la fois par l'API d'authentification et par le dashboard admin.
 */
public interface UserService {

    /** Crée un utilisateur via le formulaire d'inscription public. */
    User register(RegisterRequest request);

    /** Retourne la liste de tous les utilisateurs (admin). */
    List<User> findAll();

    User findByUserName(String userName);
    /**
     * Recherche un utilisateur par son email.
     */
    User findByEmail(String email);



    /** Recherche un utilisateur par son ID. */
    User findById(Long id);

    /**
     * Crée un utilisateur depuis le dashboard admin (sans confirmPassword).
     *
     * @param request          données du formulaire
     * @param currentUserId    ID de l'utilisateur actuellement authentifié (le "créateur"),
     *                         résolu côté contrôleur depuis le token JWT — JAMAIS depuis
     *                         un champ du corps de la requête (non fiable / falsifiable).
     */
    User create(UserFormRequest request, Long currentUserId);

    /**
     * Met à jour les informations d'un utilisateur existant.
     *
     * @param id             utilisateur à modifier
     * @param request        données du formulaire
     * @param currentUserId  ID de l'utilisateur actuellement authentifié, résolu
     *                       côté contrôleur depuis le token JWT — utilisé pour vérifier
     *                       que seul un ADMIN peut attribuer/conserver le rôle ADMIN.
     */
    User update(Long id, UserFormRequest request, Long currentUserId);

    /**
     * Supprime un utilisateur par son ID.
     *
     * @param id             utilisateur à supprimer
     * @param currentUserId  ID de l'utilisateur actuellement authentifié, résolu
     *                       côté contrôleur depuis le token JWT — utilisé pour
     *                       empêcher l'auto-suppression (voir UserServiceImpl).
     */
    void delete(Long id, Long currentUserId);

    List<User> findAllCustomer();

    /**
     * Crée un client (rôle CUSTOMER) depuis la page "Clients" du dashboard admin.
     *
     * BUGFIX : le frontend (CustomerFormModal) n'envoie que firstName/lastName/email/phone
     * et appelait POST /customers, qui n'existait sur aucun contrôleur backend → 404
     * systématique. Le nom d'utilisateur et le mot de passe (requis par le modèle User)
     * sont générés automatiquement ici.
     */
    User createCustomer(UserFormRequest request, Long currentUserId);

    /** Met à jour les informations (nom, email, téléphone) d'un client existant. */
    User updateCustomerInfo(Long id, UserFormRequest request);
}