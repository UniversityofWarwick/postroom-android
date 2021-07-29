package uk.ac.warwick.postroom.fragments

import android.graphics.Color
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import uk.ac.warwick.postroom.R
import uk.ac.warwick.postroom.domain.PostalHub
import android.os.Handler
import androidx.navigation.Navigation
import nl.dionsegijn.konfetti.KonfettiView
import nl.dionsegijn.konfetti.models.Shape


const val ARG_HUB_NAME = "paramHubName"
const val ARG_HUB_FOREGROUND = "paramHubFg"
const val ARG_HUB_BACKGROUND = "paramHubBg"

/**
 * A simple [Fragment] subclass.
 * Use the [HubInformationFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class HubInformationFragment : Fragment() {
    private lateinit var runnable: Runnable
    private var hubName: String? = null
    private var hubForeground: String? = null
    private var hubBackground: String? = null

    private var handler: Handler? = null


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            hubName = it.getString(ARG_HUB_NAME)
            hubForeground = it.getString(ARG_HUB_FOREGROUND)
            hubBackground = it.getString(ARG_HUB_BACKGROUND)
        }
        handler = Handler()
        runnable = Runnable {
            Navigation.findNavController(requireActivity(), R.id.fragment_container).navigate(
                R.id.action_hub_information_to_camera
            )
        }


    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        val rootView = inflater.inflate(R.layout.fragment_hub_information, container, false)
        val hubNameView = rootView.findViewById<TextView>(R.id.hub_name)
        hubNameView.text = arguments?.getString(ARG_HUB_NAME) ?: ""
        val fg = arguments?.getString(ARG_HUB_FOREGROUND)
        val bg = arguments?.getString(ARG_HUB_BACKGROUND)
        if (fg != null && bg != null) {
            val constraintLayout = rootView as ConstraintLayout
            constraintLayout.setBackgroundColor(Color.parseColor(bg))
            hubNameView.setTextColor(Color.parseColor(fg))
        }
        handler?.postDelayed(runnable, 2000)
        val confetti = rootView.findViewById<KonfettiView>(R.id.hub_konfetti)
        confetti.build()
            .addColors(
                Color.parseColor("#1e90ff"),
                Color.parseColor("#6b8e23"),
                Color.parseColor("#ffd700"),
                Color.parseColor("#ffc0cb"),
                Color.parseColor("#6a5acd"),
                Color.parseColor("#add8e6"),
                Color.parseColor("#ee82ee"),
                Color.parseColor("#98fb98"),
                Color.parseColor("#4682b4"),
                Color.parseColor("#f4a460"),
                Color.parseColor("#d2691e"),
                Color.parseColor("#dc143c")
            )
            .setDirection(0.0, 359.0)
            .setSpeed(15f, 15f)
            .setTimeToLive(2000L)
            .addShapes(Shape.Square, Shape.Circle)
            .addSizes(nl.dionsegijn.konfetti.models.Size(12))
            .setPosition(-50f, confetti.width + 50f, -50f, -50f)
            .streamFor(900, 500L)
        return rootView
    }

    override fun onDestroy() {
        super.onDestroy()
        handler?.removeCallbacks(runnable)
    }

    companion object {
        @JvmStatic
        fun newBundle(hub: PostalHub) =
            Bundle().apply {
                putString(ARG_HUB_NAME, hub.name)
                putString(ARG_HUB_FOREGROUND, hub.fgColour)
                putString(ARG_HUB_BACKGROUND, hub.bgColour)
            }
    }
}