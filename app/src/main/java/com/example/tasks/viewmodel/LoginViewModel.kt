package com.example.tasks.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.example.tasks.service.listener.APIListener
import com.example.tasks.service.listener.ValidationListener
import com.example.tasks.service.repository.local.SecurityPreferences
import com.example.tasks.service.constants.TaskConstants
import com.example.tasks.service.model.HeaderModel
import com.example.tasks.service.model.PriorityModel
import com.example.tasks.service.repository.PersonRepository
import com.example.tasks.service.repository.PriorityRepository
import com.example.tasks.service.repository.remote.RetrofitClient

class LoginViewModel(application: Application) : AndroidViewModel(application) {

    // Acesso a dados
    private val mSecurityPreferences = SecurityPreferences(application)
    private val mPersonRepository = PersonRepository(application)
    private val mPriorityRepository = PriorityRepository(application)

    // Login usando API
    private val mLogin = MutableLiveData<ValidationListener>()
    val login: LiveData<ValidationListener> = mLogin


    // Login usando SharedPreferences
    private val mLoggedUser = MutableLiveData<Boolean>()
    val loggedUser: LiveData<Boolean> = mLoggedUser

    /**
     * Faz login usando API
     */
    fun doLogin(email: String, password: String) {
        mPersonRepository.login(email, password, object : APIListener<HeaderModel> {
            override fun onSuccess(result: HeaderModel, statusCode: Int) {
                // Salvar dados do usuário no SharePreferences
                mSecurityPreferences.store(TaskConstants.SHARED.PERSON_KEY, result.personKey)
                mSecurityPreferences.store(TaskConstants.SHARED.TOKEN_KEY, result.token)
                mSecurityPreferences.store(TaskConstants.SHARED.PERSON_NAME, result.name)

                // Atualiza valores de Header para requisições
                RetrofitClient.addHeaders(result.personKey, result.token)

                // Informa sucesso

                mLogin.value = ValidationListener()
            }

            override fun onFailure(message: String) {
                mLogin.value = ValidationListener(message)
            }
        })
    }

    /**
     * Verifica se usuário está logado
     */
    fun verifyLoggedUser() {
        val personKey = mSecurityPreferences.get(TaskConstants.SHARED.PERSON_KEY)
        val tokenKey = mSecurityPreferences.get(TaskConstants.SHARED.TOKEN_KEY)

        // Se token e person key forem diferentes de vazio, usuário está logado
        val logged = (tokenKey != "" && personKey != "")

        // Atualiza valores de Header para requisições
        RetrofitClient.addHeaders(personKey, tokenKey)

        // Atualiza o valor
        mLoggedUser.value = logged

        // Se usuário não estiver logado, aplicação vai atualizar os dados
        if (!logged) {
            mPriorityRepository.all(object : APIListener<List<PriorityModel>> {
                override fun onSuccess(result: List<PriorityModel>, statusCode: Int) {
                    mPriorityRepository.save(result)
                }

                // Erro silencioso
                override fun onFailure(message: String) {
                    val s = ""
                }

            })
        }
    }

}