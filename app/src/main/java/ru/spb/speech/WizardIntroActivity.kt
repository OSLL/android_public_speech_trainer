package ru.spb.speech

import android.animation.ArgbEvaluator
import android.content.Intent
import android.graphics.Color
import android.graphics.PorterDuff
import android.graphics.drawable.Drawable
import android.os.Build
import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v4.app.FragmentManager
import android.support.v4.app.FragmentPagerAdapter
import android.support.v4.content.ContextCompat
import android.support.v4.graphics.drawable.DrawableCompat
import android.support.v4.view.ViewPager
import android.support.v7.app.AppCompatActivity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import kotlinx.android.synthetic.main.activity_wizard_intro.*
import kotlinx.android.synthetic.main.fragment_pager.view.*


class WizardIntroActivity : AppCompatActivity() {

    private lateinit var indicators: Array<ImageView>
    private lateinit var mSectionsPagerAdapter: SectionsPagerAdapter
    internal var page = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_wizard_intro)



        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LAYOUT_STABLE or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
            window.statusBarColor = ContextCompat.getColor(this, R.color.black_trans80)
        }

        setContentView(R.layout.activity_wizard_intro)

        mSectionsPagerAdapter = SectionsPagerAdapter(supportFragmentManager)

        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.LOLLIPOP)
            intro_btn_next.setImageDrawable(
                    tintMyDrawable(ContextCompat.getDrawable(this, R.drawable.ic_chevron_right_24dp), Color.WHITE)
            )

        indicators = arrayOf(intro_indicator_0, intro_indicator_1, intro_indicator_2)

        container.adapter = mSectionsPagerAdapter

        container.currentItem = page
        updateIndicators(page)

        val color1 = ContextCompat.getColor(this, R.color.green)
        val color2 = ContextCompat.getColor(this, R.color.orange)
        val color3 = ContextCompat.getColor(this, R.color.purple)

        val colorList = intArrayOf(color1, color2, color3)

        val evaluator = ArgbEvaluator()

        container.addOnPageChangeListener(object : ViewPager.OnPageChangeListener {
            override fun onPageScrolled(position: Int, positionOffset: Float, positionOffsetPixels: Int) {

                val colorUpdate = evaluator.evaluate(positionOffset, colorList[position], colorList[if (position == 2) position else position + 1]) as Int
                container.setBackgroundColor(colorUpdate)

            }

            override fun onPageSelected(position: Int) {

                page = position

                updateIndicators(page)

                when (position) {
                    0 -> container.setBackgroundColor(color1)
                    1 -> container.setBackgroundColor(color2)
                    2 -> container.setBackgroundColor(color3)
                }


                intro_btn_next.visibility = if (position == 2) View.GONE else View.VISIBLE
                intro_btn_finish.visibility = if (position == 2) View.VISIBLE else View.GONE
                intro_btn_skip.visibility = if (position == 2) View.GONE else View.VISIBLE


            }

            override fun onPageScrollStateChanged(state: Int) {

            }
        })

        intro_btn_next.setOnClickListener {
            page += 1
            container.setCurrentItem(page, true)
        }

        intro_btn_skip.setOnClickListener { finish() }

        intro_btn_finish.setOnClickListener {
            val toMain = Intent(this, StartPageActivity::class.java)
            startActivity(toMain)
            finish()
        }


    }


    internal fun updateIndicators(position: Int) {
        for (i in indicators.indices) {
            indicators[i].setBackgroundResource(
                    if (i == position) R.drawable.indicator_selected else R.drawable.indicator_unselected
            )
        }
    }

    class PlaceholderFragment : Fragment() {

        private lateinit var img: ImageView

        private var bgs = intArrayOf(R.drawable.training, R.drawable.research, R.drawable.chemistry)

        override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                                  savedInstanceState: Bundle?): View? {
            val messages = arrayOf(getString(R.string.wizard_intro_message_1),
                    getString(R.string.wizard_intro_message_2),
                    getString(R.string.wizard_intro_message_3))
            val rootView = inflater.inflate(R.layout.fragment_pager, container, false)
            img = rootView.findViewById(R.id.section_img)
            img.setBackgroundResource(bgs[arguments!!.getInt(ARG_SECTION_NUMBER) - 1])
            rootView.section_label.text = messages[arguments!!.getInt(ARG_SECTION_NUMBER) - 1]

            return rootView
        }

        companion object {
            private const val ARG_SECTION_NUMBER = "section_number"
            fun newInstance(sectionNumber: Int): PlaceholderFragment {
                val fragment = PlaceholderFragment()
                val args = Bundle()
                args.putInt(ARG_SECTION_NUMBER, sectionNumber)
                fragment.arguments = args
                return fragment
            }
        }


    }

    inner class SectionsPagerAdapter(fm: FragmentManager) : FragmentPagerAdapter(fm) {

        override fun getItem(position: Int): Fragment {
            return PlaceholderFragment.newInstance(position + 1)

        }

        override fun getCount(): Int {
            return 3
        }

        override fun getPageTitle(position: Int): CharSequence? {
            when (position) {
                0 -> return "SECTION 1"
                1 -> return "SECTION 2"
                2 -> return "SECTION 3"
            }
            return null
        }

    }

    companion object {
        fun tintMyDrawable(drawable: Drawable?, color: Int): Drawable {
            var mDrawable = drawable
            mDrawable = DrawableCompat.wrap(mDrawable!!)
            DrawableCompat.setTint(mDrawable!!, color)
            DrawableCompat.setTintMode(mDrawable, PorterDuff.Mode.SRC_IN)
            return mDrawable
        }
    }
}
